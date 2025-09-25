package com.dodam.plan.service;

import com.dodam.plan.config.PlanPortoneProperties;
import com.dodam.plan.service.PlanPortoneClientService.ConfirmRequest;
import com.dodam.plan.service.PlanPortoneClientService.ConfirmResponse;
import com.dodam.plan.service.PlanPortoneClientService.LookupResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
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

    // ⬇⬇⬇ 여기만 핵심 변경: confirm 60s → 15s
    private static final Duration TIMEOUT_CONFIRM = Duration.ofSeconds(15);
    private static final Duration TIMEOUT_LOOKUP  = Duration.ofSeconds(6);
    private static final Duration TIMEOUT_DEFAULT = Duration.ofSeconds(25);

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
            String billingKey = json.path("billingKey").asText(null);

            Map<String,Object> res = new HashMap<>();
            res.put("status", "ISSUED");
            if (billingKey != null) res.put("billingKey", billingKey);
            res.put("_raw", json);
            log.info("[PortOne] issue confirm 200 OK billingKey={}", billingKey);
            return res;

        } catch (WebClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                String resp = e.getResponseBodyAsString();
                try {
                    JsonNode json = mapper.readTree(resp);
                    String type = json.path("type").asText("");
                    String billingKey = json.path("billingKey").asText(null);

                    if ("BILLING_KEY_ALREADY_ISSUED".equalsIgnoreCase(type)) {
                        Map<String,Object> res = new HashMap<>();
                        res.put("status", "ISSUED");
                        if (billingKey != null) res.put("billingKey", billingKey);
                        res.put("_raw", json);
                        log.warn("[PortOne] issue confirm 409 already issued → treat as success, billingKey={}", billingKey);
                        return res;
                    }
                } catch (Exception ignore) { }
            }
            log.error("[PortOne] issue confirm {} {}", e.getRawStatusCode(), e.getResponseBodyAsString());
            throw e;
        } catch (Exception ex) {
            log.error("[PortOne] issue confirm unexpected error {}", ex.toString());
            throw new RuntimeException(ex);
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
                    .exchangeToMono(res -> res.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .map(t -> {
                                if (res.statusCode().isError()) {
                                    log.error("[PortOne] pay billing-key {} {}", res.statusCode(), t);
                                } else {
                                    log.debug("[PortOne OK] {}", t);
                                }
                                return t;
                            }))
                    .timeout(TIMEOUT_CONFIRM)
                    .onErrorResume(io.netty.handler.timeout.ReadTimeoutException.class, t -> {
                        log.warn("[PortOne] confirm READ TIMEOUT -> mark as PENDING (pid={})", req.paymentId());
                        return Mono.just("{\"status\":\"PENDING\",\"id\":\""+req.paymentId()+"\"}");
                    })
                    .onErrorResume(java.util.concurrent.TimeoutException.class, t -> {
                        log.warn("[PortOne] confirm TIMEOUT -> mark as PENDING (pid={})", req.paymentId());
                        return Mono.just("{\"status\":\"PENDING\",\"id\":\""+req.paymentId()+"\"}");
                    })
                    .block();

            JsonNode root = safeJson(raw);
            String status = pickStatus(root);

            String providerId = pick(root, "transactionId");
            if (providerId == null) providerId = pick(root.path("payment"), "transactionId");

            if (providerId == null) {
                String payId = pick(root.path("payment"), "id");
                if (payId != null && !payId.startsWith("inv")) providerId = payId;
            }

            String merchantId = pick(root, "id");
            if (merchantId == null) merchantId = pick(root.path("payment"), "id");

            if (providerId == null && merchantId != null && merchantId.startsWith("inv")) {
                providerId = translateMerchantToProvider(merchantId);
            }

            String idForLookup = providerId != null ? providerId
                                                    : (merchantId != null ? merchantId : req.paymentId());

            // 응답이 PENDING/UNKNOWN이면 즉시 1회 조회 보강
            if ((status == null || "PENDING".equals(status) || "UNKNOWN".equals(status))
                    && idForLookup != null && !idForLookup.isBlank()) {
                LookupResponse lr = lookupPayment(idForLookup);
                JsonNode j = safeJson(lr.raw());
                JsonNode node = firstPaymentNode(j);
                String s2 = pickStatus(node);
                if (s2 != null && !s2.isBlank()) status = s2;
            }

            return new ConfirmResponse(idForLookup, status != null ? status : "UNKNOWN", raw);
        } catch (Exception e) {
            log.error("[PortOne] confirmByBillingKey failed", e);
            return new ConfirmResponse(req.paymentId(), "ERROR", e.toString());
        }
    }

    @Override
    public LookupResponse lookupPayment(String paymentId) {
        try {
            Object[] resp;
            if (paymentId != null && paymentId.startsWith("inv")) {
                resp = portone.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/payments")
                                .queryParam("paymentId", paymentId)
                                .build())
                        .exchangeToMono(res -> res.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(body -> new Object[]{res.statusCode(), body}))
                        .block(TIMEOUT_LOOKUP);
            } else if (paymentId != null && paymentId.startsWith("pay_")) {
                resp = portone.get()
                        .uri(uriBuilder -> uriBuilder.path("/payments/{id}").build(paymentId))
                        .exchangeToMono(res -> res.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(body -> new Object[]{res.statusCode(), body}))
                        .block(TIMEOUT_LOOKUP);
            } else if (paymentId != null && paymentId.matches("^[0-9a-fA-F-]{8,}$")) {
                resp = portone.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/payments")
                                .queryParam("transactionId", paymentId)
                                .build())
                        .exchangeToMono(res -> res.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(body -> new Object[]{res.statusCode(), body}))
                        .block(TIMEOUT_LOOKUP);
            } else {
                resp = portone.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/payments")
                                .queryParam("paymentId", paymentId)
                                .build())
                        .exchangeToMono(res -> res.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(body -> new Object[]{res.statusCode(), body}))
                        .block(TIMEOUT_LOOKUP);
            }

            if (resp == null) return new LookupResponse(paymentId, "ERROR", "{\"error\":\"NO_RESPONSE\"}");

            HttpStatusCode sc = (HttpStatusCode) resp[0];
            String body = (String) resp[1];

            if (sc.is2xxSuccessful()) {
                JsonNode json = safeJson(body);
                JsonNode node = firstPaymentNode(json);
                String id = pick(node, "id");
                if (id == null) id = pick(node.path("payment"), "id");
                String status = pickStatus(node);
                return new LookupResponse(id != null ? id : paymentId, status != null ? status : "UNKNOWN", body);
            } else if (sc.value() == 404) {
                return new LookupResponse(paymentId, "NOT_FOUND", body);
            } else {
                return new LookupResponse(paymentId, "ERROR", body);
            }
        } catch (Exception e) {
            log.error("[PortOne] lookupPayment({}) failed", paymentId, e);
            return new LookupResponse(paymentId, "ERROR", e.toString());
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
            payment.put("currency", currency != null ? currency : "KRW");
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
                                    log.info("[PortOne OK] schedule created: {}", t);
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
    public JsonNode getPayment(String paymentId) {
        try {
            String raw = portone.get()
                    .uri(uriBuilder -> uriBuilder.path("/payments/{pid}").build(paymentId))
                    .exchangeToMono(res -> res.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .map(t -> {
                                if (res.statusCode().isError()) {
                                    log.error("[PortOne] getPayment {} {}", res.statusCode(), t);
                                } else {
                                    log.debug("[PortOne OK] getPayment {}", t);
                                }
                                return t;
                            }))
                    .timeout(TIMEOUT_DEFAULT)
                    .block();
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
                    .exchangeToMono(res -> res.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .map(t -> {
                                if (res.statusCode().isError()) {
                                    log.error("[PortOne] listPaymentsByBillingKey {} {}", res.statusCode(), t);
                                } else {
                                    log.debug("[PortOne OK] listPaymentsByBillingKey {}", t);
                                }
                                return t;
                            }))
                    .timeout(TIMEOUT_DEFAULT)
                    .block();
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
            JsonNode node = firstPaymentNode(root);
            return node.isMissingNode() ? root : node;
        } catch (Exception e) {
            log.error("[PortOne] getPaymentByOrderId({}) failed", orderId, e);
            var obj = mapper.createObjectNode();
            obj.put("status", "ERROR");
            obj.put("message", String.valueOf(e));
            return obj;
        }
    }

    // waitUntilPaid 등 나머지 헬퍼는 기존 그대로 ---------------------------------

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
        if (root.isArray()) {
            return root.size() > 0 ? root.get(0) : mapper.createObjectNode();
        }
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

    private String translateMerchantToProvider(String merchantId) {
        try {
            String body = portone.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/payments")
                            .queryParam("paymentId", merchantId)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(TIMEOUT_LOOKUP);

            JsonNode json = safeJson(body);
            JsonNode node = firstPaymentNode(json);

            String providerId = null;
            String payId = pick(node.path("payment"), "id");
            if (payId != null && payId.startsWith("pay_")) {
                providerId = payId;
            } else {
                String txId = pick(node, "transactionId");
                if (txId == null) txId = pick(node.path("payment"), "transactionId");
                if (txId != null && txId.matches("^[0-9a-fA-F-]{8,}$")) {
                    providerId = txId;
                }
            }
            log.info("[PortOne] translate inv -> provider: {} -> {}", merchantId, providerId);
            return providerId;
        } catch (Exception e) {
            log.warn("[PortOne] translateMerchantToProvider failed: {}", e.toString());
            return null;
        }
    }
}
