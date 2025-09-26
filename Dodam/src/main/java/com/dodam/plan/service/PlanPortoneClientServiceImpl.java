package com.dodam.plan.service;

import com.dodam.plan.config.PlanPortoneProperties;
import com.dodam.plan.service.PlanPortoneClientService.ConfirmRequest;
import com.dodam.plan.service.PlanPortoneClientService.ConfirmResponse;
import com.dodam.plan.service.PlanPortoneClientService.LookupResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class PlanPortoneClientServiceImpl implements PlanPortoneClientService {

    private final WebClient portone;
    private final String storeId;
    private final boolean isTest;
    private final ObjectMapper mapper = new ObjectMapper();

    private static final Duration TIMEOUT_CONFIRM = Duration.ofSeconds(15);
    private static final Duration TIMEOUT_LOOKUP  = Duration.ofSeconds(15);

    public PlanPortoneClientServiceImpl(@Qualifier("portoneWebClient") WebClient portoneWebClient,
                                        PlanPortoneProperties props) {
        this.portone = portoneWebClient;
        this.storeId = props.getStoreId();
        this.isTest = Boolean.TRUE.equals(props.getIsTest());
    }

    @Override
    public Map<String, Object> confirmIssueBillingKey(String billingIssueToken) {
        Map<String,Object> body = Map.of("billingIssueToken", billingIssueToken);
        try {
            String raw = portone.post()
                    .uri("/billing-keys/confirm")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode json = mapper.readTree(raw);
            Map<String,Object> res = new HashMap<>();
            res.put("status", "ISSUED");
            String billingKey = json.path("billingKey").asText(null);
            if (billingKey != null) res.put("billingKey", billingKey);
            res.put("_raw", json);
            log.info("[PortOne] issue confirm OK billingKey={}", billingKey);
            return res;

        } catch (Exception e) {
            log.error("[PortOne] issue confirm error {}", e.toString());
            throw new RuntimeException(e);
        }
    }

    @Override
    public ConfirmResponse confirmByBillingKey(ConfirmRequest req) {
        try {
            Map<String, Object> amount = Map.of("total", req.amountValue());
            Map<String, Object> body = new HashMap<>();
            if (storeId != null && !storeId.isBlank()) body.put("storeId", storeId);
            body.put("billingKey", req.billingKey());
            body.put("amount", amount);
            body.put("currency", req.currency());
            if (req.orderName() != null && !req.orderName().isBlank()) body.put("orderName", req.orderName());
            if (req.customerId() != null && !req.customerId().isBlank()) {
                body.put("customer", Map.of("id", req.customerId()));
            }
            if (isTest || req.isTest()) body.put("isTest", true);

            String raw = portone.post()
                    .uri(uriBuilder -> uriBuilder.path("/payments/{pid}/billing-key").build(req.paymentId()))
                    .header("Idempotency-Key", req.paymentId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(TIMEOUT_CONFIRM)
                    .onErrorResume(io.netty.handler.timeout.ReadTimeoutException.class, t -> {
                        log.warn("[PortOne] confirm READ TIMEOUT -> PENDING (pid={})", req.paymentId());
                        return Mono.just("{\"status\":\"PENDING\",\"id\":\""+req.paymentId()+"\"}");
                    })
                    .onErrorResume(java.util.concurrent.TimeoutException.class, t -> {
                        log.warn("[PortOne] confirm TIMEOUT -> PENDING (pid={})", req.paymentId());
                        return Mono.just("{\"status\":\"PENDING\",\"id\":\""+req.paymentId()+"\"}");
                    })
                    .block();

            JsonNode root = safeJson(raw);
            String status = pickStatus(root);

            // 여기서는 lookup을 강제하지 않는다(웹훅/외부 폴링이 최종 진실).
            return new ConfirmResponse(req.paymentId(), status != null ? status : "PENDING", raw);

        } catch (Exception e) {
            log.error("[PortOne] confirmByBillingKey failed", e);
            return new ConfirmResponse(req.paymentId(), "ERROR", e.toString());
        }
    }

    @Override
    public JsonNode scheduleByBillingKey(
            String paymentId,
            String billingKey,
            long amount,
            String currency,
            String customerId,
            String orderName,
            Instant timeToPayUtc
    ) {
        try {
            Map<String, Object> payment = new HashMap<>();
            payment.put("billingKey", billingKey);
            payment.put("orderName", (orderName != null && !orderName.isBlank()) ? orderName : "정기결제");
            payment.put("amount", Map.of("total", amount));
            payment.put("currency", (currency != null && !currency.isBlank()) ? currency : "KRW");
            if (storeId != null && !storeId.isBlank()) payment.put("storeId", storeId);
            if (isTest) payment.put("isTest", true);
            if (customerId != null && !customerId.isBlank()) {
                payment.put("customer", Map.of("id", customerId));
            }

            Map<String, Object> body = new HashMap<>();
            String iso = (timeToPayUtc != null)
                    ? DateTimeFormatter.ISO_INSTANT.format(timeToPayUtc)
                    : DateTimeFormatter.ISO_INSTANT.format(Instant.now().plusSeconds(30));
            body.put("payment", payment);
            body.put("timeToPay", iso);

            String raw = portone.post()
                    .uri(uriBuilder -> uriBuilder.path("/payments/{pid}/schedules").build(paymentId))
                    .header("Idempotency-Key", "sch-" + paymentId + "-" +
                            (timeToPayUtc != null ? timeToPayUtc.getEpochSecond() : Instant.now().getEpochSecond()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .exchangeToMono(res -> res.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .map(t -> {
                                if (res.statusCode().isError()) {
                                    log.error("[PortOne] schedule billing-key {} {}", res.statusCode(), t);
                                } else {
                                    log.info("[PortOne] schedule created: {}", t);
                                }
                                return t;
                            }))
                    .timeout(TIMEOUT_CONFIRM)
                    .block();

            return mapper.readTree(raw == null ? "{}" : raw);
        } catch (Exception e) {
            log.error("[PortOne] scheduleByBillingKey failed", e);
            var obj = mapper.createObjectNode();
            obj.put("status", "ERROR");
            obj.put("message", String.valueOf(e));
            return obj;
        }
    }

    @Override
    public LookupResponse lookupPayment(String paymentId) {
        try {
            String raw = portone.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/payments")
                            .queryParam("paymentId", paymentId)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(TIMEOUT_LOOKUP);

            if (raw == null) return new LookupResponse(paymentId, "ERROR", "{\"error\":\"NO_RESPONSE\"}");

            JsonNode json = safeJson(raw);
            JsonNode node = findPaymentNode(json, paymentId);

            String id = pick(node, "id");
            if (id == null) id = pick(node.path("payment"), "id");
            String status = pickStatus(node);

            if (node == null || node.isMissingNode() || id == null) {
                return new LookupResponse(paymentId, "NOT_FOUND", "{\"status\":\"NOT_FOUND\"}");
            }

            String matchedRaw = node.toString();
            return new LookupResponse(id, status != null ? status : "UNKNOWN", matchedRaw);

        } catch (Exception e) {
            log.error("[PortOne] lookupPayment({}) failed", paymentId, e);
            return new LookupResponse(paymentId, "ERROR", e.toString());
        }
    }

    @Override
    public JsonNode getPayment(String paymentId) {
        try {
            String raw = portone.get()
                    .uri(uriBuilder -> uriBuilder.path("/payments/{pid}").build(paymentId))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(TIMEOUT_LOOKUP);
            return mapper.readTree(raw == null ? "{}" : raw);
        } catch (Exception e) {
            log.error("[PortOne] getPayment failed", e);
            var obj = mapper.createObjectNode();
            obj.put("status", "ERROR");
            obj.put("message", String.valueOf(e));
            return obj;
        }
    }

    @Override
    public JsonNode listPaymentsByBillingKey(String billingKey, int size) {
        try {
            String raw = portone.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/payments")
                            .queryParam("billingKey", billingKey)
                            .queryParam("size", size <= 0 ? 10 : size)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(TIMEOUT_LOOKUP);
            return mapper.readTree(raw == null ? "{}" : raw);
        } catch (Exception e) {
            log.error("[PortOne] listPaymentsByBillingKey failed", e);
            var obj = mapper.createObjectNode();
            obj.put("status", "ERROR");
            obj.put("message", String.valueOf(e));
            return obj;
        }
    }

    @Override
    public JsonNode getPaymentByOrderId(String orderId) {
        try {
            String raw = portone.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/payments")
                            .queryParam("paymentId", orderId)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(TIMEOUT_LOOKUP);

            JsonNode root = safeJson(raw);
            JsonNode node = findPaymentNode(root, orderId);
            return node.isMissingNode() ? root : node;
        } catch (Exception e) {
            log.error("[PortOne] getPaymentByOrderId({}) failed", orderId, e);
            var obj = mapper.createObjectNode();
            obj.put("status", "ERROR");
            obj.put("message", String.valueOf(e));
            return obj;
        }
    }

    // ---- utils ----
    private JsonNode safeJson(String s) {
        try { return mapper.readTree(s == null ? "{}" : s); }
        catch (Exception e) { return mapper.createObjectNode(); }
    }
    private String pickStatus(JsonNode n) {
        String s = pick(n, "status");
        if (s == null) s = pick(n.path("payment"), "status");
        return s;
    }
    private String pick(JsonNode n, String field) {
        if (n == null) return null;
        String v = n.path(field).asText(null);
        return (v == null || v.isBlank()) ? null : v;
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
    /** 배열/래퍼가 있을 때, 특정 id/transactionId 와 '정확히' 매칭되는 노드를 찾아준다. */
    private JsonNode findPaymentNode(JsonNode root, String targetId) {
        if (root == null || root.isMissingNode()) return mapper.createObjectNode();
        if (!org.springframework.util.StringUtils.hasText(targetId)) return firstPaymentNode(root);

        // 단일
        if (!root.isArray() && !root.has("items") && !root.has("content")) {
            if (matches(root, targetId)) return root;
            JsonNode p = root.path("payment");
            if (!p.isMissingNode() && matches(p, targetId)) return root;
        }

        // 배열/items/content
        java.util.function.Function<JsonNode, JsonNode> scanArr = arr -> {
            for (JsonNode n : arr) {
                if (matches(n, targetId)) return n;
                JsonNode p = n.path("payment");
                if (!p.isMissingNode() && matches(p, targetId)) return n;
            }
            return null;
        };

        if (root.isArray()) {
            JsonNode hit = scanArr.apply(root);
            if (hit != null) return hit;
        }
        if (root.has("items") && root.path("items").isArray()) {
            JsonNode hit = scanArr.apply(root.path("items"));
            if (hit != null) return hit;
        }
        if (root.has("content") && root.path("content").isArray()) {
            JsonNode hit = scanArr.apply(root.path("content"));
            if (hit != null) return hit;
        }

        // 못 찾으면 빈 객체 (NOT_FOUND로 처리하게)
        return mapper.createObjectNode();
    }
    private boolean matches(JsonNode n, String targetId) {
        if (!org.springframework.util.StringUtils.hasText(targetId) || n == null || n.isMissingNode()) return false;
        String id  = pick(n, "id");
        String pid = pick(n.path("payment"), "id");
        String tx  = pick(n, "transactionId");
        String ptx = pick(n.path("payment"), "transactionId");
        return targetId.equals(id) || targetId.equals(pid) || targetId.equals(tx) || targetId.equals(ptx);
    }
}
