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

import java.time.LocalDateTime;
import java.util.Locale;
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

    /**
     * 웹훅은 "즉시 200" 후 비동기 처리.
     * - paymentId → invoiceId 매핑(inv{invoiceId}-ts{...}) 또는 DB에서 piUid로 탐색
     * - 안전 조회(safeLookup)로 status/receiptUrl/payKey/카드메타 보강
     * - recordAttempt() 호출 + 인보이스 상태 전이
     */
    @Async("pgWebhookExecutor")
    @Transactional
    public void process(String type, String paymentId, String transactionId, String status, String rawBody) {
        try {
            log.info("[WebhookJob] type={}, paymentId={}, txId={}, status={}", type, paymentId, transactionId, status);

            // 1) 우선 paymentId 기반으로 인보이스 찾기
            Optional<PlanInvoiceEntity> optInv = findInvoice(paymentId);
            if (optInv.isEmpty()) {
                log.warn("[WebhookJob] invoice not found by paymentId={}", paymentId);
                // 그래도 조회는 진행해서 로그 남김
            }

            // 2) 포트원 조회(상태/영수증/결제키/카드메타 확보)
            PlanLookupResult look = gateway.safeLookup(StringUtils.hasText(paymentId) ? paymentId : transactionId);
            String lookJson = (look == null || !StringUtils.hasText(look.rawJson())) ? rawBody : look.rawJson();

            // 3) 조회 결과에서 status 정규화
            String st = normUp(firstNonBlank(look.status(), status));
            boolean paid   = isPaid(st);
            boolean failed = isFailed(st);

            // 4) receiptUrl/payKey/카드 메타 파싱
            String receiptUrl = firstNonBlank(tryReceipt(lookJson), tryReceipt(rawBody));
            String resolvedPayId = firstNonBlank(look.paymentId(), transactionId, paymentId);
            String payKey = safePick(lookJson, "billingKey","billing_key","payKey");

            CardMeta cm = parseCardMeta(lookJson);
            if (StringUtils.hasText(payKey)) {
                persistCardMetaByKey(payKey, cm.bin, cm.brand, cm.last4, cm.pg);
            }

            // 5) 인보이스 상태 전이 + 시도 기록
            if (optInv.isPresent()) {
                PlanInvoiceEntity inv = optInv.get();

                // 아이템포턴시: 이미 PAID면 스킵
                if (inv.getPiStat() == PlanEnums.PiStatus.PAID) {
                    log.info("[WebhookJob] skip: already PAID (piId={}, paymentId={})", inv.getPiId(), resolvedPayId);
                    return;
                }

                if (paid) {
                    invoiceRepo.markPaidAndSetUidIfEmpty(inv.getPiId(),
                            resolvedPayId, LocalDateTime.now());
                    billing.recordAttempt(inv.getPiId(), true, null,
                            resolvedPayId, receiptUrl, lookJson);
                    log.info("[WebhookJob] PAID recorded: piId={}, receipt={}", inv.getPiId(), receiptUrl);
                } else if (failed) {
                    billing.recordAttempt(inv.getPiId(), false, "WEBHOOK:" + st,
                            resolvedPayId, receiptUrl, lookJson);
                    log.info("[WebhookJob] FAILED recorded: piId={}, status={}", inv.getPiId(), st);
                } else {
                    // 대기/수락/기타
                    billing.recordAttempt(inv.getPiId(), false, "WEBHOOK:" + (st == null ? "PENDING" : st),
                            resolvedPayId, receiptUrl, lookJson);
                    log.info("[WebhookJob] PENDING recorded: piId={}, status={}", inv.getPiId(), st);
                }
            } else {
                log.warn("[WebhookJob] no invoice mapped. paymentId={}, status={}, receipt={}",
                        resolvedPayId, st, receiptUrl);
            }

        } catch (Exception e) {
            log.error("[WebhookJob] processing error", e);
        }
    }

    /* --------------------- 내부 헬퍼 --------------------- */

    private Optional<PlanInvoiceEntity> findInvoice(String anyId) {
        try {
            if (StringUtils.hasText(anyId)) {
                // 1) piUid로 직접 검색
                Optional<PlanInvoiceEntity> o = invoiceRepo.findByPiUid(anyId);
                if (o.isPresent()) return o;

                // 2) inv{invoiceId}-ts{...} 규칙 역파싱
                Long invId = tryParseInvoiceIdFromPaymentId(anyId);
                if (invId != null) return invoiceRepo.findById(invId);
            }
        } catch (Exception ignore) {}
        return Optional.empty();
    }

    private Long tryParseInvoiceIdFromPaymentId(String paymentId) {
        try {
            if (paymentId != null && paymentId.startsWith("inv")) {
                int dash = paymentId.indexOf("-ts");
                String num = (dash > 3) ? paymentId.substring(3, dash) : paymentId.substring(3);
                return Long.parseLong(num.replaceAll("[^0-9]",""));
            }
        } catch (Exception ignore) {}
        return null;
    }

    private static String normUp(String s){ return s==null ? null : s.trim().toUpperCase(Locale.ROOT); }
    private static boolean isPaid(String s){
        String u = normUp(s);
        return "PAID".equals(u) || "SUCCEEDED".equals(u) || "SUCCESS".equals(u) || "PARTIAL_PAID".equals(u);
    }
    private static boolean isFailed(String s){
        String u = normUp(s);
        return "FAILED".equals(u) || "CANCELED".equals(u) || "CANCELLED".equals(u) || "ERROR".equals(u);
    }

    private String firstNonBlank(String... v){ if (v==null) return null; for (String s : v) if (StringUtils.hasText(s)) return s; return null; }

    private String safePick(String rawJson, String... keys) {
        try {
            JsonNode r = mapper.readTree(rawJson);
            for (String k : keys) {
                JsonNode n = getByDotted(r, k);
                if (n != null && !n.isMissingNode() && !n.isNull() && n.isValueNode()) {
                    String val = n.asText(null);
                    if (StringUtils.hasText(val)) return val;
                }
            }
        } catch (Exception ignore) {}
        return null;
    }
    private JsonNode getByDotted(JsonNode root, String dotted) {
        String[] parts = dotted.split("\\.");
        JsonNode cur = root;
        for (String p : parts) {
            if (cur == null) return null;
            cur = cur.get(p);
        }
        return cur;
    }

    private String tryReceipt(String rawJson){
        try {
            JsonNode r = mapper.readTree(rawJson);
            String r1 = safePick(rawJson, "receiptUrl","receipt.url","card.receiptUrl");
            if (StringUtils.hasText(r1)) return r1;
            JsonNode items = r.get("items");
            if (items != null && items.isArray() && items.size() > 0){
                for (JsonNode it : items) {
                    String v = safePick(it.toString(),"receiptUrl","receipt.url","card.receiptUrl");
                    if (StringUtils.hasText(v)) return v;
                }
            }
        } catch (Exception ignore) {}
        return null;
    }

    /* ===== 카드 메타 파싱/저장 ===== */
    private record CardMeta(String bin, String brand, String last4, String pg) {}

    private CardMeta parseCardMeta(String rawJson) {
        try {
            JsonNode r = mapper.readTree(rawJson);
            String bin   = safePick(rawJson, "card.bin","methodDetail.card.bin","method.card.bin");
            String brand = safePick(rawJson, "card.company","card.brand","methodDetail.brand","card.issuer","card.acquirer");
            String last4 = safePick(rawJson, "card.lastFourDigits","card.last4","methodDetail.card.last4");
            if (!StringUtils.hasText(last4)) {
                String masked = safePick(rawJson, "card.number","card.cardNumber");
                if (StringUtils.hasText(masked) && masked.length() >= 4) {
                    last4 = masked.substring(masked.length() - 4);
                }
            }
            String pg = safePick(rawJson, "pgProvider","gateway","pg","provider");
            return new CardMeta(bin, brand, sanitizeLast4(last4), pg);
        } catch (Exception e) {
            return new CardMeta(null,null,null,null);
        }
    }

    private boolean persistCardMetaByKey(String payKey, String bin, String brand, String last4, String pg) {
        if (!StringUtils.hasText(payKey)) return false;
        return paymentRepo.findByPayKey(payKey).map(p -> {
            boolean changed = false;
            if (StringUtils.hasText(bin)   && !bin.equals(p.getPayBin()))     { p.setPayBin(bin);       changed = true; }
            if (StringUtils.hasText(brand) && !brand.equals(p.getPayBrand())) { p.setPayBrand(brand);   changed = true; }
            if (StringUtils.hasText(last4) && !last4.equals(p.getPayLast4())) { p.setPayLast4(last4);   changed = true; }
            if (StringUtils.hasText(pg)    && !pg.equals(p.getPayPg()))       { p.setPayPg(pg);         changed = true; }
            if (changed) paymentRepo.save(p);
            return changed;
        }).orElse(false);
    }
    private static String sanitizeLast4(String v) {
        if (!StringUtils.hasText(v)) return null;
        String digits = v.replaceAll("\\D", "");
        if (digits.length() >= 4) return digits.substring(digits.length() - 4);
        return null;
    }
}
