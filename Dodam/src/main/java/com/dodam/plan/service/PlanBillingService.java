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

        if (log.isDebugEnabled()) {
            String respJsonPreview = (respJson == null) ? "null"
                    : (respJson.length() > 300 ? respJson.substring(0, 300) + "...(truncated)" : respJson);
            log.debug("[recordAttempt] invoiceId={}, success={}, failReason={}, respUid={}, receiptUrl={}, respJsonPreview={}",
                    invoiceId, success, failReason, respUid, receiptUrl, respJsonPreview);
        }

        // 1) 인보이스
        PlanInvoiceEntity inv = invoiceRepo.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("INVOICE_NOT_FOUND:" + invoiceId));

        // 2) Attempt 기록 (영수증 URL은 respUid 기준 정확 추출)
        String resolvedReceipt = resolveReceiptUrl(receiptUrl, respJson, respUid); // ★ respUid 전달
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
            if (reason.startsWith("LOOKUP:FAILED") || reason.startsWith("LOOKUP:CANCELED") || reason.startsWith("LOOKUP:CANCELLED")) {
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

                PlanMember pm = inv.getPlanMember();
                if (pm != null) targetPayment = pm.getPayment();

                if (targetPayment == null) {
                    String usedBillingKey = extractBillingKey(respJson);
                    if (log.isDebugEnabled()) {
                        log.debug("[recordAttempt] fallback billingKey from respJson = {}", usedBillingKey);
                    }
                    if (StringUtils.hasText(usedBillingKey)) {
                        targetPayment = paymentRepo.findByPayKey(usedBillingKey).orElse(null);
                    }
                }

                if (targetPayment == null) {
                    log.warn("[Billing] skip card meta: no target payment found (invoice={})", invoiceId);
                } else {
                    PlanCardMeta meta = pgSvc.extractCardMeta(respJson);
                    if (log.isDebugEnabled()) log.debug("[recordAttempt] extracted card meta: {}", meta);

                    // last4 보정
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

                    boolean changed = false;
                    if (meta != null) {
                        if (StringUtils.hasText(meta.getBin()) &&
                                !meta.getBin().equals(targetPayment.getPayBin())) {
                            targetPayment.setPayBin(meta.getBin());
                            changed = true;
                        }
                        if (StringUtils.hasText(meta.getBrand()) &&
                                !meta.getBrand().equals(targetPayment.getPayBrand())) {
                            targetPayment.setPayBrand(meta.getBrand());
                            changed = true;
                        }
                        if (StringUtils.hasText(meta.getLast4()) &&
                                !meta.getLast4().equals(targetPayment.getPayLast4())) {
                            targetPayment.setPayLast4(meta.getLast4());
                            changed = true;
                        }
                        if (StringUtils.hasText(meta.getPg()) &&
                                !meta.getPg().equals(targetPayment.getPayPg())) {
                            targetPayment.setPayPg(meta.getPg());
                            changed = true;
                        }
                    }

                    if (!StringUtils.hasText(targetPayment.getPayRaw()) && StringUtils.hasText(respJson)) {
                        targetPayment.setPayRaw(respJson);
                        changed = true;
                    }

                    if (changed) {
                        paymentRepo.save(targetPayment);
                        log.info("[Billing] cardMeta updated (payId={}) meta={{bin={}, brand={}, last4={}, pg={}}}",
                                targetPayment.getPayId(),
                                targetPayment.getPayBin(),
                                targetPayment.getPayBrand(),
                                targetPayment.getPayLast4(),
                                targetPayment.getPayPg());
                    } else {
                        log.info("[Billing] cardMeta skipped (no new fields) for payId={}", targetPayment.getPayId());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[Billing] save card meta failed: {}", e.toString(), e);
        }
    }

    // --- helpers ---
    /** respUid(=해당 시도 id: txId/payId/invId) 기준으로 정확히 해당 노드에서 영수증 URL 추출 */
    private String resolveReceiptUrl(String explicitReceipt, String rawJson, String targetId) {
        if (StringUtils.hasText(explicitReceipt)) return explicitReceipt;
        if (!StringUtils.hasText(rawJson)) return null;
        try {
            JsonNode root = OM.readTree(rawJson);

            // 최상위 먼저
            String v = firstNonBlank(
                    get(root, "receiptUrl"),
                    get(root, "receipt", "url"),
                    get(root, "urls", "receipt"),
                    get(root, "payment", "receiptUrl"),
                    get(root, "payment", "receipt", "url"),
                    get(root, "transactions") != null && root.path("transactions").isArray() && root.path("transactions").size() > 0
                            ? get(root.path("transactions").get(0), "receiptUrl") : null
            );
            if (StringUtils.hasText(v)) return v;

            // 정확 매칭 노드에서 재탐색
            JsonNode node = findPaymentNode(root, targetId);
            return firstNonBlank(
                    get(node, "receiptUrl"),
                    get(node, "receipt", "url"),
                    get(node, "urls", "receipt"),
                    get(node, "payment", "receiptUrl"),
                    get(node, "payment", "receipt", "url"),
                    (node.has("transactions") && node.path("transactions").isArray() && node.path("transactions").size() > 0)
                            ? get(node.path("transactions").get(0), "receiptUrl") : null
            );
        } catch (Exception ignore) { }
        return null;
    }

    private static JsonNode findPaymentNode(JsonNode root, String targetId) {
        if (root == null || root.isMissingNode()) return OM.createObjectNode();
        if (!StringUtils.hasText(targetId)) return firstPaymentNode(root);

        // 단일
        if (!root.isArray() && !root.has("items") && !root.has("content")) {
            if (matches(root, targetId)) return root;
            JsonNode p = root.path("payment");
            if (!p.isMissingNode() && matches(p, targetId)) return root;
        }

        // 배열
        if (root.isArray()) {
            for (JsonNode n : root) if (matches(n, targetId) || matches(n.path("payment"), targetId)) return n;
        }
        if (root.has("items") && root.path("items").isArray()) {
            for (JsonNode n : root.path("items")) if (matches(n, targetId) || matches(n.path("payment"), targetId)) return n;
        }
        if (root.has("content") && root.path("content").isArray()) {
            for (JsonNode n : root.path("content")) if (matches(n, targetId) || matches(n.path("payment"), targetId)) return n;
        }
        return firstPaymentNode(root);
    }

    private static boolean matches(JsonNode n, String targetId) {
        if (!StringUtils.hasText(targetId) || n == null || n.isMissingNode()) return false;
        String id  = get(n, "id");
        String pid = get(n.path("payment"), "id");
        String tx  = get(n, "transactionId");
        String ptx = get(n.path("payment"), "transactionId");
        return targetId.equals(id) || targetId.equals(pid) || targetId.equals(tx) || targetId.equals(ptx);
    }

    private static JsonNode firstPaymentNode(JsonNode root) {
        if (root == null || root.isMissingNode()) return OM.createObjectNode();
        if (root.isArray()) return root.size() > 0 ? root.get(0) : OM.createObjectNode();
        if (root.has("items") && root.path("items").isArray()) {
            JsonNode arr = root.path("items");
            return arr.size() > 0 ? arr.get(0) : OM.createObjectNode();
        }
        if (root.has("content") && root.path("content").isArray()) {
            JsonNode arr = root.path("content");
            return arr.size() > 0 ? arr.get(0) : OM.createObjectNode();
        }
        return root;
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
