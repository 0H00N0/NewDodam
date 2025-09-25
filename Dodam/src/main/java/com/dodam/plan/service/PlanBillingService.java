// src/main/java/com/dodam/plan/service/PlanBillingService.java
package com.dodam.plan.service;

import com.dodam.plan.Entity.PlanAttemptEntity;
import com.dodam.plan.Entity.PlanInvoiceEntity;
import com.dodam.plan.Entity.PlanMember;
import com.dodam.plan.Entity.PlanPaymentEntity;
import com.dodam.plan.dto.PlanCardMeta;
import com.dodam.plan.enums.PlanEnums.PattResult;
import com.dodam.plan.enums.PlanEnums.PiStatus;
import com.dodam.plan.repository.PlanAttemptRepository;
import com.dodam.plan.repository.PlanInvoiceRepository;
import com.dodam.plan.repository.PlanPaymentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanBillingService {

    private final PlanAttemptRepository attemptRepo;
    private final PlanInvoiceRepository invoiceRepo;
    private final PlanPaymentRepository paymentRepo;
    private final PlanPaymentGatewayService pgSvc;

    private static final ObjectMapper OM = new ObjectMapper();

    @Transactional
    public void recordAttempt(Long invoiceId,
                              boolean success,
                              String failReason,
                              String respUid,
                              String receiptUrl,
                              String respJson) {

        // ---- DEBUG: 입력값 요약 ----
        if (log.isDebugEnabled()) {
            String respJsonPreview = (respJson == null) ? "null"
                    : (respJson.length() > 300 ? respJson.substring(0, 300) + "...(truncated)" : respJson);
            log.debug("[recordAttempt] invoiceId={}, success={}, failReason={}, respUid={}, receiptUrl={}, respJsonPreview={}",
                    invoiceId, success, failReason, respUid, receiptUrl, respJsonPreview);
        }

        // 1) 인보이스 로드
        PlanInvoiceEntity inv = invoiceRepo.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("INVOICE_NOT_FOUND:" + invoiceId));

        // 2) Attempt 기록
        String resolvedReceipt = resolveReceiptUrl(receiptUrl, respJson);
        PlanAttemptEntity att = PlanAttemptEntity.builder()
                .invoice(inv)
                .pattResult(success ? PattResult.SUCCESS : PattResult.FAIL)
                .pattFail(success ? null : failReason)
                .pattUid(respUid)
                .pattUrl(resolvedReceipt)
                .pattResponse(respJson)
                .build();
        attemptRepo.save(att);

        log.debug("[recordAttempt] attempt saved: pattId={}, result={}, uid={}, receipt={}",
                att.getPattId(), att.getPattResult(), att.getPattUid(), att.getPattUrl());

        // 3) 인보이스 상태 전이
        PiStatus before = inv.getPiStat();
        if (success) {
            inv.setPiStat(PiStatus.PAID);
            inv.setPiPaid(LocalDateTime.now());
        } else {
            String reason = (failReason == null ? "" : failReason).toUpperCase(Locale.ROOT).trim();
            if (reason.startsWith("LOOKUP:FAILED") || reason.startsWith("LOOKUP:CANCELED")) {
                inv.setPiStat(PiStatus.FAILED);
            } else {
                if (inv.getPiStat() == null || inv.getPiStat() == PiStatus.FAILED) {
                    inv.setPiStat(PiStatus.PENDING);
                }
            }
        }
        invoiceRepo.save(inv);

        log.debug("[recordAttempt] invoice state changed: {} -> {}, piPaid={}",
                before, inv.getPiStat(), inv.getPiPaid());

        // 4) 카드 메타 저장
        try {
            if (success && StringUtils.hasText(respJson)) {
                PlanPaymentEntity targetPayment = null;

                // 인보이스 -> PlanMember -> Payment
                PlanMember pm = inv.getPlanMember();
                if (pm != null) targetPayment = pm.getPayment();

                // billingKey로 fallback
                if (targetPayment == null) {
                    String usedBillingKey = extractBillingKey(respJson);
                    log.debug("[recordAttempt] fallback billingKey = {}", usedBillingKey);
                    if (StringUtils.hasText(usedBillingKey)) {
                        Optional<PlanPaymentEntity> byKey = paymentRepo.findByPayKey(usedBillingKey);
                        if (byKey.isPresent()) targetPayment = byKey.get();
                    }
                }

                if (targetPayment == null) {
                    log.warn("[Billing] skip card meta: no target payment found (invoice={})", invoiceId);
                } else {
                    PlanCardMeta meta = pgSvc.extractCardMeta(respJson);
                    log.debug("[recordAttempt] extracted card meta: {}", meta);

                    // last4 숫자 보정
                    if (meta != null && StringUtils.hasText(meta.getLast4())) {
                        String digits = meta.getLast4().replaceAll("\\D", "");
                        if (digits.length() >= 4) {
                            meta = new PlanCardMeta(
                                    meta.getBillingKey(),
                                    meta.getBrand(),
                                    meta.getBin(),
                                    digits.substring(digits.length() - 4),
                                    meta.getPg(),
                                    false,
                                    null
                            );
                        }
                    }

                    if (meta != null) {
                        if (meta.getBin() != null) targetPayment.setPayBin(meta.getBin());
                        if (meta.getBrand() != null) targetPayment.setPayBrand(meta.getBrand());
                        if (meta.getLast4() != null) targetPayment.setPayLast4(meta.getLast4());
                        if (meta.getPg() != null) targetPayment.setPayPg(meta.getPg());

                        // 원문 보관
                        if (!StringUtils.hasText(targetPayment.getPayRaw())) {
                            targetPayment.setPayRaw(respJson);
                        }

                        paymentRepo.save(targetPayment);
                        log.info("[Billing] cardMeta updated (payId={})", targetPayment.getPayId());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[Billing] save card meta failed: {}", e.toString(), e);
        }
    }

    // --- helpers ---
    private String resolveReceiptUrl(String explicitReceipt, String rawJson) {
        if (StringUtils.hasText(explicitReceipt)) return explicitReceipt;
        if (!StringUtils.hasText(rawJson)) return null;
        try {
            JsonNode root = OM.readTree(rawJson);
            return firstNonBlank(
                    get(root, "receiptUrl"),
                    get(root, "receipt", "url"),
                    get(root, "urls", "receipt"),
                    get(root, "payment", "receiptUrl"),
                    get(root, "payment", "receipt", "url")
            );
        } catch (Exception ignore) { }
        return null;
    }

    private String extractBillingKey(String raw) {
        try {
            JsonNode root = OM.readTree(raw);
            if (root.has("items") && root.get("items").isArray() && root.get("items").size() > 0) {
                String v = n(root.get("items").get(0).path("billingKey").asText(null));
                if (StringUtils.hasText(v)) return v;
            }
            String v2 = n(root.path("payment").path("billingKey").asText(null));
            if (StringUtils.hasText(v2)) return v2;
            return n(root.path("billingKey").asText(null));
        } catch (Exception ignore) { }
        return null;
    }

    private static String get(JsonNode n, String... path) {
        if (n == null) return null;
        JsonNode cur = n;
        for (String p : path) cur = (cur == null ? null : cur.path(p));
        if (cur == null) return null;
        String v = cur.asText(null);
        return (v != null && !v.isBlank()) ? v : null;
    }

    private static String firstNonBlank(String... arr) {
        if (arr == null) return null;
        for (String s : arr) if (s != null && !s.isBlank()) return s;
        return null;
    }

    private static String n(String s){ return (s==null || s.isBlank()) ? null : s; }
}
