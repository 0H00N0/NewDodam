package com.dodam.plan.service;

import com.dodam.plan.config.PlanPortoneProperties;
import com.dodam.plan.dto.PlanCardMeta;
import com.dodam.plan.dto.PlanLookupResult;
import com.dodam.plan.dto.PlanPayResult;
import com.dodam.plan.dto.PlanPaymentLookupResult;
import com.dodam.plan.repository.PlanAttemptRepository;
import com.dodam.plan.service.PlanPortoneClientService.ConfirmRequest;
import com.dodam.plan.service.PlanPortoneClientService.ConfirmResponse;
import com.dodam.plan.service.PlanPortoneClientService.LookupResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Slf4j
@Service
public class PlanPaymentGatewayServiceImpl implements PlanPaymentGatewayService {

    private final PlanPortoneClientService portone;
    private final PlanPortoneProperties props;
    private final ObjectMapper mapper = new ObjectMapper();
    private final PlanAttemptRepository attemptRepo;

    public PlanPaymentGatewayServiceImpl(
            @Qualifier("planPortoneClientServiceImpl") PlanPortoneClientService portone,
            PlanPortoneProperties props,
            PlanAttemptRepository attemptRepo
    ) {
        this.portone = portone;
        this.props = props;
        this.attemptRepo = attemptRepo;
    }

    @Override
    public PlanPayResult payByBillingKey(String paymentId, String billingKey, long amount, String customerId) {
        return payByBillingKey(
                paymentId,
                billingKey,
                amount,
                props.getCurrency() != null ? props.getCurrency() : "KRW",
                "Dodam Subscription",
                props.getStoreId(),
                customerId,
                props.getChannelKey()
        );
    }

    @Override
    public PlanPayResult payByBillingKey(
            String paymentId,
            String billingKey,
            long amount,
            String currency,
            String orderName,
            String storeId,
            String customerId,
            String channelKey
    ) {
        // 1) 승인
        ConfirmRequest req = new ConfirmRequest(
                paymentId, billingKey, amount, currency, customerId, orderName,
                Boolean.TRUE.equals(props.getIsTest())
        );
        ConfirmResponse res = portone.confirmByBillingKey(req);

        String status = norm(res.status());
        boolean success = isPaidStatus(status);

        String providerPaymentUid = n(res.id());
        String receiptUrl = null;

        String rawToStore = res.raw();

        // 2) 영수증 URL 스니핑 (정확 매칭 시도)
        try {
            if (rawToStore != null && rawToStore.startsWith("{")) {
                String target = n(providerPaymentUid) != null ? providerPaymentUid : paymentId;
                JsonNode root = mapper.readTree(rawToStore);
                JsonNode node = findPaymentNode(root, target);

                receiptUrl = firstNonBlank(
                        get(root, "receiptUrl"),
                        get(root, "receipt", "url"),
                        get(root, "urls", "receipt"),
                        get(root, "transactions") != null && root.path("transactions").isArray() && root.path("transactions").size() > 0
                                ? get(root.path("transactions").get(0), "receiptUrl") : null,
                        get(node, "receiptUrl"),
                        get(node, "receipt", "url"),
                        get(node, "urls", "receipt"),
                        get(node, "transactions") != null && node.path("transactions").isArray() && node.path("transactions").size() > 0
                                ? get(node.path("transactions").get(0), "receiptUrl") : null
                );
            }
        } catch (Exception ignore) {}

        String payUidToStore = n(providerPaymentUid) != null ? providerPaymentUid : paymentId;

        // 3) 간단 보강 폴링(6s)
        if (!success) {
            final String loopKey = resolvePaymentId(firstNonBlank(providerPaymentUid, paymentId));
            final long until = System.currentTimeMillis() + 6_000L;

            while (System.currentTimeMillis() < until) {
                try {
                    LookupResponse lr = portone.lookupPayment(loopKey);
                    String st = norm(lr.status());

                    if (isPaidStatus(st)) {
                        success = true;
                        status = st;
                        providerPaymentUid = n(lr.id());
                        payUidToStore = n(providerPaymentUid) != null ? providerPaymentUid : paymentId;

                        if (lr.raw() != null && !lr.raw().isBlank()) {
                            rawToStore = lr.raw();
                            try {
                                JsonNode r = mapper.readTree(lr.raw());
                                JsonNode node = findPaymentNode(r, payUidToStore);
                                String rcp = firstNonBlank(
                                        get(r, "receiptUrl"),
                                        get(r, "receipt", "url"),
                                        get(r, "urls", "receipt"),
                                        get(node, "receiptUrl"),
                                        get(node, "receipt", "url"),
                                        get(node, "urls", "receipt")
                                );
                                if (rcp != null) receiptUrl = rcp;
                            } catch (Exception ignore) {}
                        }
                        break;
                    }
                    if (isFailedStatus(st)) {
                        status = st;
                        break;
                    }
                } catch (Exception ignore) { }

                try { Thread.sleep(700); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt(); break;
                }
            }
        }

        return new PlanPayResult(success, payUidToStore, receiptUrl, success ? null : status, status, rawToStore);
    }

    /**
     * 공급사 조회에 사용할 paymentId 보정
     * - inv{invoiceId}-u{uuid} 형태면 invoiceId로 DB에서 최신 providerUid를 역참조
     * - 그렇지 않으면 그대로 사용
     */
    private String resolvePaymentId(String anyId) {
        if (!StringUtils.hasText(anyId)) return anyId;
        if (anyId.startsWith("inv")) {
            Long invoiceId = extractInvoiceId(anyId);
            if (invoiceId != null) {
                return attemptRepo.findLatestPaymentUidByInvoiceId(invoiceId)
                        .filter(StringUtils::hasText)
                        .orElse(anyId);
            }
        }
        return anyId;
    }

    @Override
    public PlanLookupResult safeLookup(String paymentId) {
        try {
            String pid = resolvePaymentId(paymentId);
            if (!StringUtils.hasText(pid)) {
                return new PlanLookupResult(false, paymentId, "NOT_FOUND", "{\"error\":\"no providerId for invoice\"}");
            }
            LookupResponse r = portone.lookupPayment(pid);
            boolean ok = isPaidStatus(r.status());
            return new PlanLookupResult(ok, r.id(), r.status(), r.raw());
        } catch (Exception e) {
            return new PlanLookupResult(false, paymentId, "ERROR", "{\"error\":\"" + e + "\"}");
        }
    }

    @Override
    public PlanLookupResult lookup(String paymentId) {
        String pid = resolvePaymentId(paymentId);
        if (!StringUtils.hasText(pid)) {
            return new PlanLookupResult(false, paymentId, "NOT_FOUND", "{\"error\":\"no providerId for invoice\"}");
        }
        var r = portone.lookupPayment(pid);
        boolean ok = isPaidStatus(r.status());
        return new PlanLookupResult(ok, r.id(), r.status(), r.raw());
    }

    @Override
    public PlanPaymentLookupResult lookupPayment(String paymentId) {
        String pid = resolvePaymentId(paymentId);
        try {
            if (!StringUtils.hasText(pid)) {
                return new PlanPaymentLookupResult(paymentId, "NOT_FOUND", "{\"error\":\"no providerId for invoice\"}", HttpStatus.OK);
            }
            var r = portone.lookupPayment(pid);
            return new PlanPaymentLookupResult(r.id(), r.status(), r.raw(), HttpStatus.OK);
        } catch (Exception e) {
            return new PlanPaymentLookupResult(paymentId, "ERROR", e.toString(), HttpStatus.BAD_GATEWAY);
        }
    }

    // -------- 카드 메타 추출 --------
    @Override
    public PlanCardMeta extractCardMeta(String rawJson) {
        if (!StringUtils.hasText(rawJson)) {
            return new PlanCardMeta(null, null, null, null, null, false, null);
        }
        try {
            JsonNode root = mapper.readTree(rawJson);
            JsonNode node = firstPaymentNode(root); // 기본: 첫 노드

            // 카드정보가 없는 경우 items 전체 훑어 첫 카드정보 있는 노드 사용
            if (get(node, "method", "card", "brand") == null && root.has("items") && root.path("items").isArray()) {
                for (JsonNode n : root.path("items")) {
                    if (get(n, "method", "card", "brand") != null) { node = n; break; }
                }
            }

            String billingKey = firstNonBlank(
                    get(node, "billingKey"),
                    get(node, "method", "billingKey"),
                    get(node, "method", "card", "billingKey"),
                    get(node, "billing", "billingKey"),
                    get(node, "payment", "method", "billingKey")
            );

            String bin   = firstNonBlank(get(node, "method", "card", "bin"),   get(node, "card", "bin"));
            String brand = firstNonBlank(get(node, "method", "card", "brand"), get(node, "card", "brand"));
            String last4 = firstNonBlank(
                    get(node, "method", "card", "last4"),
                    last4FromMasked(get(node, "method", "card", "number")),
                    last4FromMasked(get(node, "card", "number"))
            );
            String pg = firstNonBlank(
                    get(node, "pgProvider"), get(node, "pgCompany"),
                    get(node, "payment", "pgProvider"), get(node, "payment", "pgCompany")
            );

            if (log.isDebugEnabled()) {
                log.debug("[extractCardMeta] billingKey={}, bin={}, brand={}, last4={}, pg={}",
                        billingKey, bin, brand, last4, pg);
            }

            return new PlanCardMeta(billingKey, brand, bin, last4, pg, false, null);
        } catch (Exception e) {
            log.warn("[extractCardMeta] parse failed: {}", e.toString());
            return new PlanCardMeta(null, null, null, null, null, false, null);
        }
    }

    // ---- 유틸 ----
    /**
     * inv{digits}-u{uuid...} 에서 digits 부분만 추출
     */
    private Long extractInvoiceId(String uid) {
        if (!StringUtils.hasText(uid)) return null;
        try {
            String num = uid.replaceFirst("^inv","")
                    .split("-u")[0]
                    .replaceAll("[^0-9]","");
            return Long.parseLong(num);
        } catch (Exception e) {
            return null;
        }
    }

    private static String get(JsonNode n, String... path) {
        if (n == null) return null;
        JsonNode cur = n;
        for (String p : path) cur = (cur == null ? null : cur.path(p));
        if (cur == null) return null;
        String v = cur.asText(null);
        return (v != null && !v.isBlank()) ? v : null;
    }
    private static String last4FromMasked(String masked) {
        if (masked == null) return null;
        String digits = masked.replaceAll("\\D", "");
        return digits.length() >= 4 ? digits.substring(digits.length() - 4) : null;
    }
    private static String firstNonBlank(String... arr) {
        if (arr == null) return null;
        for (String s : arr) if (s != null && !s.isBlank()) return s;
        return null;
    }

    private JsonNode firstPaymentNode(JsonNode root) {
        if (root == null || root.isMissingNode()) return mapper.createObjectNode();
        if (root.isArray()) return root.size() > 0 ? root.get(0) : mapper.createObjectNode();
        if (root.has("items") && root.path("items").isArray()) {
            JsonNode arr = root.path("items");
            return arr.size() > 0 ? arr.get(0) : mapper.createObjectNode();
        }
        if (root.has("content") && root.path("content").isArray()) {
            JsonNode arr = root.path("content");
            return arr.size() > 0 ? arr.get(0) : mapper.createObjectNode();
        }
        return root;
    }

    private JsonNode findPaymentNode(JsonNode root, String targetId) {
        if (root == null || root.isMissingNode() || !StringUtils.hasText(targetId)) return firstPaymentNode(root);
        // 단일
        if (!root.isArray() && !root.has("items") && !root.has("content")) {
            if (matches(root, targetId)) return root;
            if (matches(root.path("payment"), targetId)) return root;
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

    private boolean matches(JsonNode n, String targetId) {
        if (!StringUtils.hasText(targetId) || n == null || n.isMissingNode()) return false;
        String id  = get(n, "id");
        String pid = get(n.path("payment"), "id");
        String tx  = get(n, "transactionId");
        String ptx = get(n.path("payment"), "transactionId");
        return targetId.equals(id) || targetId.equals(pid) || targetId.equals(tx) || targetId.equals(ptx);
    }

    private String n(String s) { return (s == null || s.isBlank()) ? null : s; }
    private String norm(String v){ return (v==null) ? "" : v.trim().toUpperCase(Locale.ROOT); }
    private boolean isPaidStatus(String status) {
        String s = norm(status);
        return s.equals("PAID") || s.equals("SUCCEEDED") || s.equals("SUCCESS") || s.equals("PARTIAL_PAID");
        // 필요 시 CAPTURED 등 추가
    }
    private boolean isFailedStatus(String status) {
        String s = norm(status);
        return s.equals("FAILED") || s.equals("CANCELED") || s.equals("CANCELLED");
    }
}
