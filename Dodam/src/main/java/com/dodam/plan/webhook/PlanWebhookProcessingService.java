package com.dodam.plan.webhook;

import com.dodam.plan.Entity.PlanInvoiceEntity;
import com.dodam.plan.Entity.PlanPaymentEntity;
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

    /* ======================== 결제 웹훅 ========================= */
    @Async("webhookExecutor")
    @Transactional
    public void process(String type, String paymentId, String transactionId, String status, String rawBody) {
        try {
            log.info("[WebhookJob] type={}, paymentId={}, txId={}, status={}", type, paymentId, transactionId, status);

            PlanLookupResult look = gateway.safeLookup(StringUtils.hasText(paymentId) ? paymentId : transactionId);
            String lookJson = (look != null && StringUtils.hasText(look.rawJson())
                    && !look.rawJson().contains("\"error\":\"no paymentId\"")) ? look.rawJson() : null;
            String richJson = preferRicherJson(lookJson, rawBody);

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

            Optional<PlanInvoiceEntity> optInv = findInvoiceByOrderFirst(orderId, resolvedPayId);
            final String stStrict = normUp(firstNonBlank(
                    safePickStrict(richJson, "status", "payment.status", "data.status", "items[0].status"),
                    status
            ));
            final String stLookup = normUp((look != null ? look.status() : null));
            String st = (StringUtils.hasText(stStrict) ? stStrict : stLookup);
            if (!StringUtils.hasText(st) || "NOT_FOUND".equals(st)) {
                String stFromItems = normUp(firstNonBlank(safePickStrict(richJson, "items[0].status")));
                if (StringUtils.hasText(stFromItems)) st = stFromItems;
            }
            boolean paid = isPaid(st);
            boolean failed = isFailed(st);

            final String receiptUrl = tryReceipt(richJson);
            final String payKey = safePick(richJson, "billingKey","billing_key","payKey");
            final CardMeta cm = parseCardMeta(richJson);
            if (StringUtils.hasText(payKey)) {
                persistCardMetaByKey(payKey, cm.bin, cm.brand, cm.last4, cm.pg);
            }

            if (optInv.isEmpty()) {
                BigDecimal amount = tryBigDecimal(firstNonBlank(
                        safePick(richJson, "amount.total","amount","data.amount.total","items[0].amount.total")));
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
                log.info("[WebhookJob] non-terminal ({}) → skip recordAttempt", st);
            }

        } catch (Exception e) {
            log.error("[WebhookJob] processing error", e);
        }
    }

    /* ======================== 빌링키 웹훅 ========================= */
    @Transactional
    public void handleBillingKeyEvent(JsonNode root, boolean issued) {
        try {
            if (root == null || root.isEmpty()) {
                log.info("[BillingKeyWebhook] root is empty → skip");
                return;
            }

            JsonNode data = root.path("data");
            String billingKey = firstNonBlank(
                    data.path("id").asText(null),
                    data.path("billingKey").path("id").asText(null),
                    data.path("billing_key_id").asText(null)
            );
            String customerKey = firstNonBlank(
                    data.path("customerKey").asText(null),
                    data.path("customer").path("id").asText(null),
                    data.path("customer_id").asText(null)
            );

            if (!StringUtils.hasText(billingKey)) {
                log.warn("[BillingKeyWebhook] missing billingKey id");
                return;
            }

            JsonNode detail = gateway.getBillingKey(billingKey);
            String brand = text(detail.path("card"), "brand", "brandName");
            String bin = text(detail.path("card"), "bin");
            String last4 = text(detail.path("card"), "last4", "numberLast4");
            String pg = text(detail, "pgProvider", "provider", "pg");
            String status = text(detail, "status");
            String mid = customerKey != null ? customerKey : "unknown";

            Optional<PlanPaymentEntity> found = paymentRepo.findByMidAndPayKey(mid, billingKey);
            PlanPaymentEntity entity = found.orElseGet(PlanPaymentEntity::new);
            entity.setMid(mid);
            entity.setPayKey(billingKey);
            entity.setPayCustomer(customerKey);
            entity.setPayBrand(brand);
            entity.setPayBin(bin);
            entity.setPayLast4(last4);
            entity.setPayPg(pg);
            entity.setPayRaw(detail.toPrettyString());
            if (entity.getPayCreatedAt() == null)
                entity.setPayCreatedAt(LocalDateTime.now());

            paymentRepo.save(entity);
            log.info("[BillingKeyWebhook] {} saved mid={} billingKey={} brand={} last4={}",
                    issued ? "ISSUED" : "READY", mid, billingKey, brand, last4);

        } catch (Exception e) {
            log.error("[BillingKeyWebhook] process failed: {}", e.getMessage(), e);
        }
    }

    /* ======================== 공통 유틸 ========================= */
    private static String text(JsonNode n, String... keys) {
        for (String k : keys) {
            String v = n.path(k).asText(null);
            if (StringUtils.hasText(v)) return v;
        }
        return null;
    }

    private String preferRicherJson(String a, String b) {
        int sa = richnessScore(a);
        int sb = richnessScore(b);
        return (sb > sa) ? b : a;
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

    private String safePick(String rawJson, String... keys){
        try{
            if(!StringUtils.hasText(rawJson))return null;
            JsonNode root=mapper.readTree(rawJson);
            String v=pickFromNode(root,keys); if(StringUtils.hasText(v)) return v;
        }catch(Exception ignore){}
        return null;
    }

    private String safePickStrict(String rawJson, String... keys){
        try{
            if(!StringUtils.hasText(rawJson))return null;
            JsonNode root=mapper.readTree(rawJson);
            return pickFromNode(root,keys);
        }catch(Exception ignore){}
        return null;
    }

    private String pickFromNode(JsonNode root,String...keys){
        for(String k:keys){
            JsonNode n=getByDotted(root,k);
            if(n!=null&&!n.isMissingNode()&&!n.isNull()&&n.isValueNode()){
                String val=n.asText(null);
                if(StringUtils.hasText(val)) return val;
            }
        }
        return null;
    }

    private JsonNode getByDotted(JsonNode root,String dotted){
        String[] parts=dotted.split("\\.");
        JsonNode cur=root;
        for(String p:parts){
            if(cur==null) return null;
            int idxStart=p.indexOf('[');
            if(idxStart>-1&&p.endsWith("]")){
                String field=p.substring(0,idxStart);
                String idxStr=p.substring(idxStart+1,p.length()-1);
                cur=cur.get(field);
                if(cur==null||!cur.isArray()) return null;
                int i;
                try{i=Integer.parseInt(idxStr);}catch(Exception e){return null;}
                cur=(i>=0&&i<cur.size())?cur.get(i):null;
            } else cur=cur.get(p);
        }
        return cur;
    }

    private BigDecimal tryBigDecimal(String s){
        try{
            if(!StringUtils.hasText(s)) return null;
            String cleaned=s.replaceAll("[^0-9.\\-]","");
            if(!StringUtils.hasText(cleaned)) return null;
            return new BigDecimal(cleaned);
        }catch(Exception e){return null;}
    }

    private String tryReceipt(String rawJson){
        try{
            if(!StringUtils.hasText(rawJson)) return null;
            JsonNode root=mapper.readTree(rawJson);
            String direct=safePick(rawJson,
                    "receiptUrl","receipt.url","pgResponse.receipt.url","pgResponse.receiptUrl");
            if(StringUtils.hasText(direct)) return direct;
        }catch(Exception e){
            log.warn("[WebhookJob] tryReceipt parse error: {}",e.toString());
        }
        return null;
    }

    private record CardMeta(String bin,String brand,String last4,String pg){}

    private CardMeta parseCardMeta(String rawJson){
        try{
            String last4=firstNonBlank(
                    safePick(rawJson,"method.card.last4","card.last4","card.lastFourDigits"),
                    tail4Digits(safePick(rawJson,"card.number")));
            String bin=safePick(rawJson,"card.bin");
            String brand=safePick(rawJson,"card.brand");
            String pg="TossPayments";
            return new CardMeta(bin,brand,last4,pg);
        }catch(Exception e){
            return new CardMeta(null,null,null,"TossPayments");
        }
    }

    private boolean persistCardMetaByKey(String payKey,String bin,String brand,String last4,String pg){
        if(!StringUtils.hasText(payKey)) return false;
        return paymentRepo.findByPayKey(payKey).map(p->{
            boolean changed=false;
            if(StringUtils.hasText(bin)&&!bin.equals(p.getPayBin())){p.setPayBin(bin);changed=true;}
            if(StringUtils.hasText(brand)&&!brand.equals(p.getPayBrand())){p.setPayBrand(brand);changed=true;}
            if(StringUtils.hasText(last4)&&!last4.equals(p.getPayLast4())){p.setPayLast4(last4);changed=true;}
            if(StringUtils.hasText(pg)&&!pg.equals(p.getPayPg())){p.setPayPg(pg);changed=true;}
            if(changed) paymentRepo.save(p);
            return changed;
        }).orElse(false);
    }

    private static String tail4Digits(String s){
        if(!StringUtils.hasText(s)) return null;
        String d=s.replaceAll("\\D","");
        return (d.length()>=4)?d.substring(d.length()-4):null;
    }

    private static LocalDateTime nowUtc(){ return LocalDateTime.now(ZoneOffset.UTC); }
}
