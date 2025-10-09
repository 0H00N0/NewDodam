// src/main/java/com/dodam/plan/webhook/PlanWebhookProcessingService.java
package com.dodam.plan.webhook;

import com.dodam.plan.Entity.PlanInvoiceEntity;
import com.dodam.plan.dto.PlanLookupResult;
import com.dodam.plan.enums.PlanEnums;
import com.dodam.plan.repository.PlanInvoiceRepository;
import com.dodam.plan.repository.PlanPaymentRepository;
import com.dodam.plan.service.PlanBillingService;
import com.dodam.plan.service.PlanPaymentGatewayService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanWebhookProcessingService {

    private final PlanPaymentGatewayService gateway;
    private final PlanBillingService billing;
    private final PlanInvoiceRepository invoiceRepo;
    private final PlanPaymentRepository paymentRepo;
    private final ObjectMapper mapper;

    @Async("webhookExecutor")
    @Transactional
    public void process(String type, String paymentId, String transactionId, String status, String rawBody) {
        try {
            log.info("[WebhookJob] type={}, paymentId={}, txId={}, status={}", type, paymentId, transactionId, status);

            // 0) PortOne 조회 (정상 JSON만 사용)
            PlanLookupResult look = gateway.safeLookup(StringUtils.hasText(paymentId) ? paymentId : transactionId);
            String lookJson = (look != null && StringUtils.hasText(look.rawJson())
                    && !look.rawJson().contains("\"error\":\"no paymentId\"")) ? look.rawJson() : null;

            // ▶ richJson: 영수증/카드필드가 더 많이 담긴 것을 우선
            String richJson = preferRicherJson(lookJson, rawBody);

            // 1) 결제/주문 식별자
            final String resolvedPayId = firstNonBlank(
                    (look != null ? look.paymentId() : null),
                    safePick(richJson, "data.paymentId", "id", "paymentId"),
                    transactionId, paymentId
            );

            String orderId = firstNonBlank(
                    safePick(richJson, "orderId","order.id","order.orderId","data.orderId","data.order.id","id","pgResponse.orderId"),
                    safePick(richJson, "items[0].id","items[0].orderId","items[0].order.id"),
                    safePick(richJson, "data.paymentId")
            );

            // 2) 인보이스 찾기
            Optional<PlanInvoiceEntity> optInv = findInvoiceByOrderFirst(orderId, resolvedPayId);

            // 3) 상태 판정
            final String stStrict = normUp(firstNonBlank(
                    safePickStrict(richJson, "status", "payment.status", "data.status", "items[0].status"),
                    status
            ));
            final String stLookup = normUp((look != null ? look.status() : null));
            String st = (StringUtils.hasText(stStrict) ? stStrict : stLookup);

            if (!StringUtils.hasText(st) || "NOT_FOUND".equals(st)) {
                String stFromItems = normUp(firstNonBlank(
                        safePickStrict(richJson, "items[0].status")
                ));
                if (StringUtils.hasText(stFromItems)) st = stFromItems;
            }

            boolean paid   = isPaid(st);
            boolean failed = isFailed(st);

            // 3-1) 영수증 URL (1차: richJson)
            final String receiptUrl = tryReceipt(richJson);

            // 3-2) 카드메타 (billingKey 기준 선반영)
            final String payKey = safePick(richJson, "billingKey","billing_key","payKey");
            final CardMeta cm   = parseCardMeta(richJson);
            if (StringUtils.hasText(payKey)) {
                persistCardMetaByKey(payKey, cm.bin, cm.brand, cm.last4, cm.pg);
            }

            // 4) 금액/시간 보조매칭
            if (optInv.isEmpty()) {
                BigDecimal amount = tryBigDecimal(firstNonBlank(
                        safePick(richJson, "amount.total", "amount", "data.amount.total", "items[0].amount.total")
                ));
                if (amount != null) {
                    Optional<PlanInvoiceEntity> alt = invoiceRepo.findRecentPendingSameAmount(amount, Duration.ofMinutes(20));
                    if (alt.isPresent()) {
                        log.info("[WebhookJob] fallback matched by amount/time: {}", amount);
                        optInv = alt;
                        if ("NOT_FOUND".equals(st)) st = "PAID";
                    }
                }
            }

            if (optInv.isEmpty()) {
                log.warn("[WebhookJob] no invoice mapped. paymentId={}, orderId={}, status={}, receipt={}",
                        resolvedPayId, orderId, st, receiptUrl);
                return;
            }

            final PlanInvoiceEntity inv = optInv.get();
            log.info("[WebhookDebug] mapped piId={}, resolvedPayId={}, orderId={}, status={}",
                    inv.getPiId(), resolvedPayId, orderId, st);

            // 이미 PAID → UID 백필 + 종결 attempt 1회
            if (inv.getPiStat() == PlanEnums.PiStatus.PAID) {
                if (!StringUtils.hasText(inv.getPiUid()) && StringUtils.hasText(resolvedPayId)) {
                    invoiceRepo.markPaidAndSetUidIfEmpty(inv.getPiId(), resolvedPayId, nowUtc());
                }
                billing.recordAttempt(inv.getPiId(), true, null, resolvedPayId, receiptUrl, richJson);
                return;
            }

            if (paid) {
                invoiceRepo.markPaidAndSetUidIfEmpty(inv.getPiId(), resolvedPayId, nowUtc());
                billing.recordAttempt(inv.getPiId(), true, null, resolvedPayId, receiptUrl, richJson);
            } else if (failed) {
                billing.recordAttempt(inv.getPiId(), false, "WEBHOOK:" + st, resolvedPayId, receiptUrl, richJson);
            } else {
                // 비종결 상태는 스킵 (중복 attempt 방지)
                log.info("[WebhookJob] non-terminal ({}) → skip recordAttempt", st);
            }

        } catch (Exception e) {
            log.error("[WebhookJob] processing error", e);
        }
    }

    /* ---------- JSON 우선 선택: 영수증/카드/pgTxId가 많은 쪽 ---------- */
    private String preferRicherJson(String a, String b) {
        int sa = richnessScore(a);
        int sb = richnessScore(b);
        if (sb > sa) return b;
        return a != null ? a : b;
    }
    private int richnessScore(String j) {
        if (!StringUtils.hasText(j)) return 0;
        int s = 0;
        String low = j.toLowerCase(Locale.ROOT);
        if (low.contains("receipt")) s += 3;
        if (low.contains("pgtxid")) s += 3;
        if (low.contains("\"method\":{\"type\":\"paymentmethodcard\"")) s += 2;
        if (low.contains("\"card\"")) s += 2;
        if (low.contains("\"items\"")) s += 1;
        return s;
    }

    /* ======== 헬퍼 ======== */
    private Optional<PlanInvoiceEntity> findInvoiceByOrderFirst(String orderId, String paymentId) {
        if (StringUtils.hasText(orderId)) {
            Long invId = tryParseInvoiceIdFromOrderId(orderId);
            if (invId != null) {
                Optional<PlanInvoiceEntity> byId = invoiceRepo.findById(invId);
                if (byId.isPresent()) return byId;
            }
        }
        return findInvoice(paymentId);
    }

    private Optional<PlanInvoiceEntity> findInvoice(String anyId) {
        try {
            if (StringUtils.hasText(anyId)) {
                Optional<PlanInvoiceEntity> o = invoiceRepo.findByPiUid(anyId.trim());
                if (o.isPresent()) return o;
                Long invId = tryParseInvoiceIdFromPaymentId(anyId.trim());
                if (invId != null) return invoiceRepo.findById(invId);
            }
        } catch (Exception ignore) {}
        return Optional.empty();
    }

    private Long tryParseInvoiceIdFromPaymentId(String paymentId) {
        try {
            if (paymentId != null && paymentId.startsWith("inv")) {
                int dash = paymentId.indexOf("-");
                String num = (dash > 3) ? paymentId.substring(3, dash) : paymentId.substring(3);
                return Long.parseLong(num.replaceAll("[^0-9]",""));
            }
        } catch (Exception ignore) {}
        return null;
    }
    private Long tryParseInvoiceIdFromOrderId(String orderId) { return tryParseInvoiceIdFromPaymentId(orderId); }

    private static String normUp(String s){ return s==null ? null : s.trim().toUpperCase(Locale.ROOT); }
    private static boolean isPaid(String s){ String u=normUp(s);
        return "PAID".equals(u)||"SUCCEEDED".equals(u)||"SUCCESS".equals(u)||"DONE".equals(u)||"COMPLETED".equals(u); }
    private static boolean isFailed(String s){ String u=normUp(s);
        return "FAILED".equals(u)||"CANCELED".equals(u)||"CANCELLED".equals(u)||"ERROR".equals(u); }

    private String firstNonBlank(String... v){ if(v==null)return null; for(String s:v) if(StringUtils.hasText(s)) return s; return null; }

    private String safePick(String rawJson, String... keys){ try{
        if(!StringUtils.hasText(rawJson))return null; JsonNode root=mapper.readTree(rawJson);
        String v=pickFromNode(root,keys); if(StringUtils.hasText(v)) return v;
        String[] withItems0=new String[keys.length]; for(int i=0;i<keys.length;i++) withItems0[i]="items[0]."+keys[i];
        v=pickFromNode(root,withItems0); if(StringUtils.hasText(v)) return v;
        if(root.isObject()){
            for (Iterator<Map.Entry<String, JsonNode>> it = root.fields(); it.hasNext(); ) {
                Map.Entry<String, JsonNode> e = it.next();
                JsonNode n = e.getValue();
                if (n.isTextual()) {
                    String t = n.asText();
                    if (t.startsWith("{") || t.startsWith("[")) {
                        JsonNode nested = mapper.readTree(t);
                        v = pickFromNode(nested, keys);
                        if (StringUtils.hasText(v)) return v;
                        v = pickFromNode(nested, withItems0);
                        if (StringUtils.hasText(v)) return v;
                    }
                }
            }
        }
    }catch(Exception ignore){} return null; }

    private String safePickStrict(String rawJson, String... keys){
        try{ if(!StringUtils.hasText(rawJson))return null; JsonNode root=mapper.readTree(rawJson);
            String v=pickFromNode(root,keys); if(StringUtils.hasText(v)) return v;
            String[] withItems0=new String[keys.length]; for(int i=0;i<keys.length;i++) withItems0[i]="items[0]."+keys[i];
            return pickFromNode(root,withItems0);}catch(Exception ignore){} return null; }

    private String pickFromNode(JsonNode root,String...keys){
        for(String k:keys){ JsonNode n=getByDotted(root,k);
            if(n!=null&&!n.isMissingNode()&&!n.isNull()&&n.isValueNode()){
                String val=n.asText(null); if(StringUtils.hasText(val)) return val; }}
        return null; }

    private JsonNode getByDotted(JsonNode root,String dotted){
        String[] parts=dotted.split("\\."); JsonNode cur=root;
        for(String p:parts){ if(cur==null) return null;
            int idxStart=p.indexOf('[');
            if(idxStart>-1&&p.endsWith("]")){ String field=p.substring(0,idxStart);
                String idxStr=p.substring(idxStart+1,p.length()-1); cur=cur.get(field);
                if(cur==null||!cur.isArray()) return null; int i; try{i=Integer.parseInt(idxStr);}catch(Exception e){return null;}
                cur=(i>=0&&i<cur.size())?cur.get(i):null; } else cur=cur.get(p);}
        return cur; }

    private BigDecimal tryBigDecimal(String s){
        try{ if(!StringUtils.hasText(s)) return null;
            String cleaned=s.replaceAll("[^0-9.\\-]",""); if(!StringUtils.hasText(cleaned)) return null;
            return new BigDecimal(cleaned);}catch(Exception e){return null;}
    }

    private String tryReceipt(String rawJson){
        try {
            if (!StringUtils.hasText(rawJson)) return null;
            JsonNode root = mapper.readTree(rawJson);

            String direct = safePick(rawJson,
                    "receiptUrl","receipt.url","card.receiptUrl",
                    "pgResponse.receipt.url","pgResponse.receiptUrl","pgResponse.receipt.redirectUrl",
                    "items[0].pgResponse.receipt.url","items[0].receiptUrl");
            if (StringUtils.hasText(direct)) return direct;

            String txId = firstNonBlank(
                    safePick(rawJson, "payment.pgTxId","pgTxId","items[0].payment.pgTxId","items[0].pgTxId"));
            if (StringUtils.hasText(txId)) {
                return "https://dashboard-sandbox.tosspayments.com/receipt/redirection?transactionId="+txId+"&ref=PX";
            }

            JsonNode pgResp = root.path("pgResponse");
            if (!pgResp.isMissingNode()) {
                if (pgResp.isTextual()) {
                    JsonNode nested = mapper.readTree(pgResp.asText());
                    String v = pickFromNode(nested, "receipt.url","receiptUrl","receipt.redirectUrl");
                    if (StringUtils.hasText(v)) return v;
                    JsonNode t = nested.get("transactionId");
                    if (t != null && t.isValueNode() && StringUtils.hasText(t.asText()))
                        return "https://dashboard-sandbox.tosspayments.com/receipt/redirection?transactionId="+t.asText()+"&ref=PX";
                } else {
                    String v = pickFromNode(pgResp, "receipt.url","receiptUrl","receipt.redirectUrl");
                    if (StringUtils.hasText(v)) return v;
                    JsonNode t = pgResp.get("transactionId");
                    if (t != null && t.isValueNode() && StringUtils.hasText(t.asText()))
                        return "https://dashboard-sandbox.tosspayments.com/receipt/redirection?transactionId="+t.asText()+"&ref=PX";
                }
            }

            JsonNode items = root.get("items");
            if (items != null && items.isArray()) {
                for (JsonNode it : items) {
                    String v = safePick(it.toString(),"pgResponse.receipt.url","pgResponse.receiptUrl","receipt.url","receiptUrl");
                    if (StringUtils.hasText(v)) return v;
                    String itTx = firstNonBlank(safePick(it.toString(),"payment.pgTxId","pgTxId"));
                    if (StringUtils.hasText(itTx))
                        return "https://dashboard-sandbox.tosspayments.com/receipt/redirection?transactionId="+itTx+"&ref=PX";
                    JsonNode pr = it.path("pgResponse");
                    if (!pr.isMissingNode()) {
                        if (pr.isTextual()) {
                            JsonNode nested = mapper.readTree(pr.asText());
                            v = pickFromNode(nested, "receipt.url","receiptUrl","receipt.redirectUrl");
                            if (StringUtils.hasText(v)) return v;
                            JsonNode t = nested.get("transactionId");
                            if (t != null && t.isValueNode() && StringUtils.hasText(t.asText()))
                                return "https://dashboard-sandbox.tosspayments.com/receipt/redirection?transactionId="+t.asText()+"&ref=PX";
                        } else {
                            v = pickFromNode(pr, "receipt.url","receiptUrl","receipt.redirectUrl");
                            if (StringUtils.hasText(v)) return v;
                            JsonNode t = pr.get("transactionId");
                            if (t != null && t.isValueNode() && StringUtils.hasText(t.asText()))
                                return "https://dashboard-sandbox.tosspayments.com/receipt/redirection?transactionId="+t.asText()+"&ref=PX";
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[WebhookJob] tryReceipt parse error: {}", e.toString());
        }
        return null;
    }

    private record CardMeta(String bin,String brand,String last4,String pg){}
    private CardMeta parseCardMeta(String rawJson) {
        try {
            String last4 = firstNonBlank(
                    safePick(rawJson, "method.card.last4","card.last4","card.lastFourDigits","items[0].method.card.last4"),
                    tail4Digits(safePick(rawJson, "method.card.number","card.number","items[0].method.card.number"))
            );
            String bin = firstNonBlank(
                    safePick(rawJson, "method.card.bin","card.bin","items[0].method.card.bin")
            );
            String brand = firstNonBlank(
                    safePick(rawJson, "items[0].method.card.name"),
                    safePick(rawJson, "method.card.company","card.company","items[0].method.card.issuer","items[0].method.card.company"),
                    safePick(rawJson, "method.card.brand","card.brand","items[0].method.card.brand")
            );
            String pg = "TossPayments";
            return new CardMeta(bin, brand, last4, pg);
        } catch (Exception e) {
            return new CardMeta(null, null, null, "TossPayments");
        }
    }
    private boolean persistCardMetaByKey(String payKey,String bin,String brand,String last4,String pg){
        if(!StringUtils.hasText(payKey)) return false;
        return paymentRepo.findByPayKey(payKey).map(p->{ boolean changed=false;
            if(StringUtils.hasText(bin)&&!bin.equals(p.getPayBin())){p.setPayBin(bin);changed=true;}
            if(StringUtils.hasText(brand)&&!brand.equals(p.getPayBrand())){p.setPayBrand(brand);changed=true;}
            if(StringUtils.hasText(last4)&&!last4.equals(p.getPayLast4())){p.setPayLast4(last4);changed=true;}
            if(StringUtils.hasText(pg)&&!pg.equals(p.getPayPg())){p.setPayPg(pg);changed=true;}
            if(changed) paymentRepo.save(p); return changed;}).orElse(false);
    }
    private static String tail4Digits(String s){ if(!StringUtils.hasText(s)) return null;
        String d=s.replaceAll("\\D",""); return (d.length()>=4)?d.substring(d.length()-4):null; }
    private static LocalDateTime nowUtc(){ return LocalDateTime.now(ZoneOffset.UTC); }
}
