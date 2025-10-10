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
import java.util.Optional;

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

    /* =========================================================================
       결제 승인 (UUID 가짜 paymentId 폐기 → orderId 기반 승인 → paymentId 확보)
       인터페이스 시그니처 유지: 첫 인자(String)는 이제 "orderId"로 해석
    ========================================================================= */

    @Override
    public PlanPayResult payByBillingKey(String orderId, String billingKey, long amount, String customerId) {
        return payByBillingKey(
                orderId,
                billingKey,
                amount,
                defaultCurrency(),
                "Dodam Subscription",
                props.getStoreId(),
                customerId,
                props.getChannelKey()
        );
    }

    @Override
    public PlanPayResult payByBillingKey(
            String orderId,
            String billingKey,
            long amount,
            String currency,
            String orderName,
            String storeId,
            String customerId,
            String channelKey
    ) {
        // 1) PortOne /payments/confirm : orderId + billingKey 로 승인
        ConfirmRequest req = new ConfirmRequest(
                orderId,                                // 🔴 기존 paymentId 자리에 "orderId" 전달
                billingKey,
                amount,
                n(currency, defaultCurrency()),
                customerId,
                n(orderName, "Dodam Subscription"),
                Boolean.TRUE.equals(props.getIsTest())
        );
        ConfirmResponse res = portone.confirmByBillingKey(req);

        String status = norm(res != null ? res.status() : null);
        String providerPaymentUid = res != null ? n(res.id()) : null;
        String rawToStore = res != null ? res.raw() : null;
        String receiptUrl = null;

        // 2) 응답에서 receiptUrl 추출 시도
        if (hasText(rawToStore)) {
            try {
                JsonNode root = mapper.readTree(rawToStore);
                receiptUrl = receiptFrom(root);
                if (!hasText(providerPaymentUid)) {
                    // 일부 응답엔 id가 없을 수 있어 후속 조회 대비
                    providerPaymentUid = jst(root, "id");
                }
            } catch (Exception ignore) {}
        }

        boolean success = isPaidStatus(status);

        // 3) 응답에 paymentId가 비어있으면, orderId "정확 일치"로 paymentId 확보
        if (!hasText(providerPaymentUid)) {
            try {
                Optional<JsonNode> hit = portone.findPaymentByExactOrderId(orderId);
                if (hit.isPresent()) {
                    JsonNode it = hit.get();
                    providerPaymentUid = it.path("id").asText(null);
                    if (!hasText(receiptUrl)) {
                        receiptUrl = firstNonBlank(
                                it.path("receiptUrl").asText(null),
                                jst(it, "receipt.url")
                        );
                    }
                }
            } catch (Exception e) {
                log.warn("[payByBillingKey] findPaymentByExactOrderId failed: {}", e.toString());
            }
        }

        // 4) 간단 폴백 조회(최대 6초) — 승인 직후 반영 지연 보정
        if (!success) {
            final String pid = n(providerPaymentUid, null);
            final long until = System.currentTimeMillis() + 6_000L;

            while (System.currentTimeMillis() < until) {
                try {
                    if (!hasText(pid)) break;
                    LookupResponse lr = portone.lookupPayment(pid);
                    String st = norm(lr.status());
                    if (isPaidStatus(st)) {
                        success = true;
                        status = st;
                        if (hasText(lr.raw())) {
                            rawToStore = lr.raw();
                            try {
                                JsonNode r = mapper.readTree(lr.raw());
                                String rcp = receiptFrom(r);
                                if (hasText(rcp)) receiptUrl = rcp;
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

        // 5) 결과 반환 (paymentId는 PortOne 실ID)
        return new PlanPayResult(
                success,
                providerPaymentUid,
                receiptUrl,
                success ? null : status,
                status,
                rawToStore
        );
    }

    /* =========================================================================
       조회 (anyId: paymentId 또는 orderId 모두 허용)
    ========================================================================= */

    @Override
    public PlanLookupResult safeLookup(String anyId) {
        try {
            String pid = resolveToPaymentId(anyId);
            if (!hasText(pid)) {
                return new PlanLookupResult(false, anyId, "NOT_FOUND", "{\"error\":\"no paymentId\"}");
            }
            LookupResponse r = portone.lookupPayment(pid);
            boolean ok = isPaidStatus(r.status());
            return new PlanLookupResult(ok, r.id(), r.status(), r.raw());
        } catch (Exception e) {
            return new PlanLookupResult(false, anyId, "ERROR", "{\"error\":\"" + e + "\"}");
        }
    }

    @Override
    public PlanLookupResult lookup(String anyId) {
        String pid = resolveToPaymentId(anyId);
        if (!hasText(pid)) {
            return new PlanLookupResult(false, anyId, "NOT_FOUND", "{\"error\":\"no paymentId\"}");
        }
        var r = portone.lookupPayment(pid);
        boolean ok = isPaidStatus(r.status());
        return new PlanLookupResult(ok, r.id(), r.status(), r.raw());
    }

    @Override
    public PlanPaymentLookupResult lookupPayment(String anyId) {
        try {
            String pid = resolveToPaymentId(anyId);
            if (!hasText(pid)) {
                return new PlanPaymentLookupResult(anyId, "NOT_FOUND", "{\"error\":\"no paymentId\"}", HttpStatus.OK);
            }
            var r = portone.lookupPayment(pid);
            return new PlanPaymentLookupResult(r.id(), r.status(), r.raw(), HttpStatus.OK);
        } catch (Exception e) {
            return new PlanPaymentLookupResult(anyId, "ERROR", e.toString(), HttpStatus.BAD_GATEWAY);
        }
    }

    /* =========================================================================
       카드 메타 추출
    ========================================================================= */

    @Override
    public PlanCardMeta extractCardMeta(String rawJson) {
        if (!hasText(rawJson)) {
            return new PlanCardMeta(null, null, null, null, null, false, null);
        }
        try {
            JsonNode root = mapper.readTree(rawJson);
            JsonNode node = firstPaymentNode(root);

            String billingKey = firstNonBlank(
                    jst(node, "billingKey"),
                    jst(node, "method.card.billingKey"),
                    jst(node, "payment.method.billingKey")
            );
            String bin   = firstNonBlank(jst(node, "method.card.bin"),   jst(node, "card.bin"));
            String brand = firstNonBlank(jst(node, "method.card.brand"), jst(node, "card.brand"));
            String last4 = firstNonBlank(
                    jst(node, "method.card.last4"),
                    last4FromMasked(jst(node, "method.card.number")),
                    last4FromMasked(jst(node, "card.number"))
            );
            String pg = firstNonBlank(
                    jst(node, "pgProvider"), jst(node, "pgCompany"),
                    jst(node, "payment.pgProvider"), jst(node, "payment.pgCompany")
            );

            return new PlanCardMeta(billingKey, brand, bin, last4, pg, false, null);
        } catch (Exception e) {
            log.warn("[extractCardMeta] parse failed: {}", e.toString());
            return new PlanCardMeta(null, null, null, null, null, false, null);
        }
    }

    /* =========================================================================
       내부 유틸
    ========================================================================= */

    /** anyId(paymentId 또는 orderId)를 PortOne paymentId로 정규화 */
    private String resolveToPaymentId(String anyId) {
        if (!hasText(anyId)) return null;

        // 1) 새 orderId: inv{digits}-ts{epoch}
        if (anyId.matches("^inv\\d+-ts\\d+$")) {
            try {
                return portone.findPaymentByExactOrderId(anyId)
                        .map(n -> n.path("id").asText(null))
                        .orElse(null);
            } catch (Exception ignore) {}
        }

        // 2) 레거시: inv{digits}-u{uuid} → DB에서 providerUid 역참조
        if (anyId.matches("^inv\\d+-u[0-9a-fA-F-]{8,}$")) {
            Long invoiceId = extractInvoiceIdFromLegacy(anyId);
            if (invoiceId != null) {
                return attemptRepo.findLatestPaymentUidByInvoiceId(invoiceId)
                        .filter(StringUtils::hasText)
                        .orElse(null);
            }
            return null;
        }

        // 3) 그 외는 paymentId로 간주
        return anyId;
    }

    private Long extractInvoiceIdFromLegacy(String uid) {
        try {
            if (!hasText(uid)) return null;
            String num = uid.replaceFirst("^inv","").split("-u")[0].replaceAll("[^0-9]","");
            return Long.parseLong(num);
        } catch (Exception e) { return null; }
    }

    private String defaultCurrency() { return props.getCurrency() != null ? props.getCurrency() : "KRW"; }
    private boolean hasText(String s){ return s != null && !s.isBlank(); }
    private String n(String v){ return hasText(v) ? v : null; }
    private String n(String v, String d){ return hasText(v) ? v : d; }
    private String norm(String v){ return (v==null) ? "" : v.trim().toUpperCase(Locale.ROOT); }
    private boolean isPaidStatus(String status) {
        String s = norm(status);
        return s.equals("PAID") || s.equals("SUCCEEDED") || s.equals("SUCCESS") || s.equals("PARTIAL_PAID");
    }
    private boolean isFailedStatus(String status) {
        String s = norm(status);
        return s.equals("FAILED") || s.equals("CANCELED") || s.equals("CANCELLED");
    }

    private String jst(JsonNode n, String dotted) {
        if (n == null) return null;
        String[] p = dotted.split("\\.");
        JsonNode cur = n;
        for (String k: p) cur = cur.path(k);
        return cur.isMissingNode() || cur.isNull() ? null : cur.asText();
    }

    private String firstNonBlank(String... arr) {
        if (arr == null) return null;
        for (String s : arr) if (hasText(s)) return s;
        return null;
    }

    private String last4FromMasked(String masked) {
        if (!hasText(masked)) return null;
        String digits = masked.replaceAll("\\D", "");
        return digits.length() >= 4 ? digits.substring(digits.length() - 4) : null;
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

    private String receiptFrom(JsonNode root) {
        if (root == null) return null;
        // 널리 쓰이는 경로 우선
        String r = firstNonBlank(
                jst(root, "receiptUrl"),
                jst(root, "receipt.url"),
                jst(root, "payment.receiptUrl")
        );
        if (hasText(r)) return r;

        // transactions[0]
        JsonNode txs = root.path("transactions");
        if (txs.isArray() && txs.size() > 0) {
            String t = jst(txs.get(0), "receiptUrl");
            if (hasText(t)) return t;
        }

        // items[0]
        JsonNode items = root.path("items");
        if (items.isArray() && items.size() > 0) {
            String t = firstNonBlank(
                    jst(items.get(0), "receiptUrl"),
                    jst(items.get(0), "receipt.url")
            );
            if (hasText(t)) return t;
        }
        return null;
    }

    @Override
    public JsonNode confirmBilling(
            String orderId,
            String billingKey,
            long amount,
            String currency,
            String orderName,
            String customerId
    ) {
        try {
            // PortOne 표준 승인 엔드포인트 (/payments/confirm) 호출
            PlanPortoneClientService.ConfirmResponse r = portone.confirmByOrderId(
                    orderId,
                    billingKey,
                    amount,
                    (currency != null && !currency.isBlank()) ? currency : defaultCurrency(),
                    customerId,
                    (orderName != null && !orderName.isBlank()) ? orderName : "Dodam Subscription"
            );
            String raw = (r != null) ? r.raw() : null;
            return mapper.readTree(raw == null ? "{}" : raw);
        } catch (Exception e) {
            log.error("[Gateway] confirmBilling(orderId={}) failed", orderId, e);
            var obj = mapper.createObjectNode();
            obj.put("status", "ERROR");
            obj.put("message", String.valueOf(e));
            return obj;
        }
    }

    @Override
    public JsonNode findByOrderId(String orderId) {
        // 가능하면 컨트롤러/서비스 레벨에서는 정확 일치 API를 쓰자.
        // 이 메서드는 하위 호환용으로 남겨둔다.
        JsonNode res = portone.findPaymentByOrderId(orderId);
        return (res != null) ? res : mapper.createObjectNode();
    }

    /** orderId 정확 일치 조회 - PortOne 클라이언트에 위임 */
    public Optional<JsonNode> findPaymentByExactOrderId(String orderId) {
        try {
            return portone.findPaymentByExactOrderId(orderId);
        } catch (Exception e) {
            log.warn("[Gateway] findPaymentByExactOrderId({}) failed: {}", orderId, e.toString());
            return Optional.empty();
        }
    }
    
    @Override
    public JsonNode getBillingKey(String billingKey) {
        try {
            String url = props.getBaseUrl() + "/billing-keys/" + billingKey;
            var client = org.springframework.web.client.RestClient.create();
            String json = client.get()
                    .uri(url)
                    .header("Authorization", props.authHeader())
                    .retrieve()
                    .body(String.class);
            return mapper.readTree(json);
        } catch (Exception e) {
            log.error("[Gateway] getBillingKey({}) failed: {}", billingKey, e.toString());
            throw new RuntimeException("PortOne billing-key 조회 실패");
        }
    }
}
