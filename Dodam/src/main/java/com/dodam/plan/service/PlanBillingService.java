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

    /**
     * 결제 시도 기록 + 인보이스 상태 전이 + (성공 시) 카드메타 안전 업데이트
     *
     * 규칙 요약
     * - success=true            → PattResult.SUCCESS, pattFail=null, 인보이스=PAID
     * - failReason=ACCEPTED/PENDING → PattResult.FAIL 이지만 pattFail=false (보류), 인보이스=PENDING 유지
     * - 그 외 명확 실패/취소    → PattResult.FAIL, pattFail=true, 인보이스=FAILED
     */
    @Transactional
    public void recordAttempt(Long invoiceId,
                              boolean success,
                              String failReason,   // "ACCEPTED","PENDING","FAILED" 등
                              String respUid,      // provider paymentId / txId 등
                              String receiptUrl,   // 영수증 URL(명시적 전달 시)
                              String respJson) {   // 게이트웨이 원문 JSON
        if (log.isDebugEnabled()) {
            String preview = (respJson == null) ? "null"
                    : (respJson.length() > 300 ? respJson.substring(0, 300) + "...(truncated)" : respJson);
            log.debug("[recordAttempt] invoiceId={}, success={}, failReason={}, respUid={}, receiptUrl={}, respJsonPreview={}",
                    invoiceId, success, failReason, respUid, receiptUrl, preview);
        }

        // 1) 인보이스 조회
        PlanInvoiceEntity inv = invoiceRepo.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("INVOICE_NOT_FOUND:" + invoiceId));

        // 2) Attempt 저장 (ACCEPTED/PENDING은 '보류'로 기록: enum은 FAIL이지만 pattFail=false)
        String resolvedReceipt = resolveReceiptUrl(receiptUrl, respJson, respUid);

        final String reasonUC = (failReason == null ? "" : failReason).toUpperCase(Locale.ROOT).trim();
        // ✅ 대기(보류)로 볼 상태 확장
        final boolean isPendingLike =
                "ACCEPTED".equals(reasonUC) ||
                "PENDING".equals(reasonUC)  ||
                "READY".equals(reasonUC)    ||
                "NOT_FOUND".equals(reasonUC)||
                reasonUC.startsWith("LOOKUP:PENDING") ||
                reasonUC.startsWith("LOOKUP:NOT_FOUND");

        // 저장 로직은 동일 (성공이면 SUCCESS, 아니면 FAIL이지만 보류 시 pattFail=null)
        final PattResult pattResult = success ? PattResult.SUCCESS : PattResult.FAIL;
        final String    pattFailStr = success ? null : (isPendingLike ? null : reasonUC);

        PlanAttemptEntity att = PlanAttemptEntity.builder()
                .invoice(inv)
                .pattResult(pattResult)
                .pattFail(pattFailStr)   // 엔티티가 String(또는 Boolean)이라면 기존 타입에 맞춰 사용하세요
                .pattUid(respUid)
                .pattUrl(StringUtils.hasText(resolvedReceipt) ? resolvedReceipt : null)
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
            if (reasonUC.startsWith("LOOKUP:FAILED") || reasonUC.startsWith("LOOKUP:CANCELED") || reasonUC.startsWith("LOOKUP:CANCELLED")
                    || "FAILED".equals(reasonUC) || "CANCELED".equals(reasonUC) || "CANCELLED".equals(reasonUC)) {
                inv.setPiStat(PiStatus.FAILED);
            } else if (isPendingLike) {
                inv.setPiStat(PiStatus.PENDING); // ✅ 보류 유지
            } else {
                // 그 외 모호하면 기존 상태 없을 때만 PENDING으로
                if (inv.getPiStat() == null) inv.setPiStat(PiStatus.PENDING);
            }
        }
        invoiceRepo.save(inv);
        log.debug("[recordAttempt] invoice state changed: {} -> {}, piPaid={}", before, inv.getPiStat(), inv.getPiPaid());

        // 4) 카드 메타 저장 (성공시에만, 그리고 인보이스의 PlanMember.payment에 한정)
        try {
            if (success && StringUtils.hasText(respJson)) {
                PlanMember pm = inv.getPlanMember();
                PlanPaymentEntity targetPayment = (pm != null) ? pm.getPayment() : null;

                if (targetPayment == null) {
                    log.warn("[Billing] skip card meta: invoice has no bound payment (invoiceId={})", invoiceId);
                    return;
                }

                // 게이트웨이 응답에서 billingKey 추출
                String usedBillingKey = extractBillingKey(respJson);
                if (log.isDebugEnabled()) log.debug("[recordAttempt] used billingKey from gateway = {}", usedBillingKey);

                // 응답 billingKey와 인보이스 결제수단의 key가 다르면 업데이트 금지
                if (StringUtils.hasText(usedBillingKey) &&
                        StringUtils.hasText(targetPayment.getPayKey()) &&
                        !usedBillingKey.equals(targetPayment.getPayKey())) {
                    log.warn("[Billing] card meta mismatch -> skip update (invoiceId={}, target.payId={}, target.key={}, resp.key={})",
                            invoiceId, targetPayment.getPayId(), mask(targetPayment.getPayKey()), mask(usedBillingKey));
                    return;
                }

                // 카드 메타 추출 및 보정
                PlanCardMeta meta = pgSvc.extractCardMeta(respJson);
                if (log.isDebugEnabled()) log.debug("[recordAttempt] extracted card meta: {}", meta);

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

                // 원문 저장(처음 한 번만)
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
        } catch (Exception e) {
            log.warn("[Billing] save card meta failed: {}", e.toString(), e);
        }
    }

    // ===== helpers =====
    private String resolveReceiptUrl(String explicitReceipt, String rawJson, String targetId) {
        if (StringUtils.hasText(explicitReceipt)) return explicitReceipt;
        if (!StringUtils.hasText(rawJson)) return null;
        try {
            JsonNode root = OM.readTree(rawJson);

            String v = firstNonBlank(
                    get(root, "receiptUrl"),
                    get(root, "receipt", "url"),
                    get(root, "urls", "receipt"),
                    get(root, "payment", "receiptUrl"),
                    get(root, "payment", "receipt", "url"),
                    (root.has("transactions") && root.path("transactions").isArray() && root.path("transactions").size() > 0)
                            ? get(root.path("transactions").get(0), "receiptUrl") : null
            );
            if (StringUtils.hasText(v)) return v;

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

        if (!root.isArray() && !root.has("items") && !root.has("content")) {
            if (matches(root, targetId)) return root;
            JsonNode p = root.path("payment");
            if (!p.isMissingNode() && matches(p, targetId)) return root;
        }

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
            JsonNode arr = root.path("items"); return arr.size() > 0 ? arr.get(0) : OM.createObjectNode();
        }
        if (root.has("content") && root.path("content").isArray()) {
            JsonNode arr = root.path("content"); return arr.size() > 0 ? arr.get(0) : OM.createObjectNode();
        }
        return root;
    }

    private String extractBillingKey(String raw) {
        try {
            JsonNode root = OM.readTree(raw);
            if (root.has("items") && root.get("items").isArray() && root.get("items").size() > 0) {
                String v = n(root.get(0).path("billingKey").asText(null));
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

    private static String mask(String v) {
        if (!StringUtils.hasText(v)) return v;
        return (v.length() <= 8) ? "****" + v : v.substring(0, 4) + "****" + v.substring(v.length() - 4);
    }
}
