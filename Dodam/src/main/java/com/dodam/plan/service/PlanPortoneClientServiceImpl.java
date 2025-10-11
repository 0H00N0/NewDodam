package com.dodam.plan.service;

import com.dodam.plan.config.PlanPortoneProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service("planPortoneClientServiceImpl")
@RequiredArgsConstructor
public class PlanPortoneClientServiceImpl implements PlanPortoneClientService {

    private final WebClient portoneWebClient;   // Config에서 주입
    private final PlanPortoneProperties props;  // 필요 시 사용
    private final ObjectMapper mapper = new ObjectMapper();

    // PortOne 권장: 최소 60초 읽기 타임아웃
    private Duration timeout() { return Duration.ofSeconds(60); }
    private static String enc(String v) { return URLEncoder.encode(v, StandardCharsets.UTF_8); }
    private boolean hasText(String s) { return StringUtils.hasText(s); }

    /** dotted path 텍스트 추출 (없으면 null) */
    private String jst(JsonNode n, String dotted) {
        if (n == null) return null;
        String[] p = dotted.split("\\.");
        JsonNode cur = n;
        for (String k : p) cur = cur.path(k);
        return (cur.isMissingNode() || cur.isNull()) ? null : cur.asText();
    }

    // ======================== 즉시 결제(빌링키) ========================
    // V2: POST /payments/{paymentId}/billing-key
    @Override
    public ConfirmResponse confirmByOrderId(
            String orderId,
            String billingKey,
            long amount,
            String currency,
            String customerId,
            String orderName
    ) {
        try {
            final String useCurr = hasText(currency) ? currency : "KRW";
            final String useName = hasText(orderName) ? orderName : "Dodam Subscription";

            ObjectNode body = mapper.createObjectNode();
            ObjectNode amountNode = mapper.createObjectNode();
            amountNode.put("total", amount);
            body.set("amount", amountNode);
            body.put("currency", useCurr);
            body.put("orderName", useName);
            body.put("billingKey", billingKey);

            // customer.id (V2 스키마)
            ObjectNode customer = mapper.createObjectNode();
            if (hasText(customerId)) customer.put("id", customerId);
            body.set("customer", customer);

            String raw = portoneWebClient
                    .post()
                    .uri("/payments/{paymentId}/billing-key", orderId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(body.toString()))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(timeout())
                    .doOnSubscribe(s -> log.info("[PortOne] POST https://api.portone.io/payments/{}/billing-key  Authorization=PortOne ****", orderId))
                    .block();

            JsonNode r = mapper.readTree(raw == null ? "{}" : raw);
            String status = jst(r, "status");
            String id     = jst(r, "id"); // 결제ID

            log.info("[PortOne] billing-key pay id={} status={} amount={} paidAt={}",
                    hasText(id) ? id : orderId, status, jst(r, "amount.total"), jst(r, "paidAt"));

            return new ConfirmResponse(
                    id,
                    hasText(status) ? status : "PENDING",
                    raw
            );
        } catch (Exception e) {
            log.warn("[PortOne] billing-key pay TIMEOUT/ERROR -> PENDING (orderId={})", orderId, e);
            String raw = "{\"status\":\"PENDING\",\"orderId\":\"" + orderId + "\"}";
            return new ConfirmResponse(null, "PENDING", raw);
        }
    }

    @Override
    public ConfirmResponse confirmByBillingKey(ConfirmRequest req) {
        return confirmByOrderId(
                req.paymentId(),     // 여기서는 결제건ID(=우리 orderId)를 그대로 사용
                req.billingKey(),
                req.amountValue(),
                req.currency(),
                req.customerId(),
                req.orderName()
        );
    }

    // ======================== 단건 조회 ========================
    @Override
    public LookupResponse lookupPayment(String paymentId) {
        try {
            String raw = portoneWebClient.get()
                    .uri("/payments/" + enc(paymentId))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(timeout())
                    .doOnSubscribe(s -> log.info("[PortOne] GET https://api.portone.io/payments/{}  Authorization=PortOne ****", paymentId))
                    .block();

            JsonNode r = mapper.readTree(raw == null ? "{}" : raw);
            return new LookupResponse(
                    r.path("id").asText(null),
                    r.path("status").asText(null),
                    raw
            );
        } catch (Exception e) {
            String raw = "{\"status\":\"ERROR\",\"message\":\"" + e + "\"}";
            return new LookupResponse(paymentId, "ERROR", raw);
        }
    }

    // ======================== 리스트 조회 (정확매칭) ========================
    @Override
    public Optional<JsonNode> findPaymentByExactOrderId(String orderId) {
        try {
            String listRaw = portoneWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/payments")
                            .queryParam("orderId", orderId)
                            .queryParam("size", 50)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(timeout())
                    .doOnSubscribe(s -> log.info("[PortOne] GET https://api.portone.io/payments?orderId={}&size=50  Authorization=PortOne ****", orderId))
                    .block();

            if (!StringUtils.hasText(listRaw)) return Optional.empty();

            log.info("[PortOne] /payments?orderId={}&size=50 응답: {}", orderId, listRaw);

            JsonNode root = mapper.readTree(listRaw);
            JsonNode items = root.path("items");
            if (!items.isArray()) return Optional.empty();

            for (JsonNode it : items) {
                String id = it.path("id").asText(null);

                // pgResponse가 문자열/오브젝트 두 가지 케이스 처리
                String pgOrderId = null;
                JsonNode pgResp = it.get("pgResponse");
                if (pgResp != null && !pgResp.isNull()) {
                    if (pgResp.isObject()) {
                        pgOrderId = pgResp.path("orderId").asText(null);
                    } else if (pgResp.isTextual()) {
                        try {
                            JsonNode parsed = mapper.readTree(pgResp.asText());
                            pgOrderId = parsed.path("orderId").asText(null);
                        } catch (Exception ignore) { }
                    }
                }

                if (orderId.equals(id) || orderId.equals(pgOrderId)) {
                    return Optional.of(it);
                }
            }
            return Optional.empty();

        } catch (Exception e) {
            log.warn("[PortOne] findPaymentByExactOrderId error: {}", e.toString());
            return Optional.empty();
        }
    }

    // ======================== 유틸 조회 ========================
    @Override
    public JsonNode findPaymentByOrderId(String orderId) {
        try {
            String listRaw = portoneWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/payments")
                            .queryParam("orderId", orderId)
                            .queryParam("size", 10)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(timeout())
                    .doOnSubscribe(s -> log.info("[PortOne] GET https://api.portone.io/payments?orderId={}&size=10  Authorization=PortOne ****", orderId))
                    .block();
            return mapper.readTree(StringUtils.hasText(listRaw) ? listRaw : "{}");
        } catch (Exception e) {
            return mapper.createObjectNode();
        }
    }

    @Override
    public JsonNode getPayment(String paymentId) {
        try {
            String raw = portoneWebClient.get()
                    .uri("/payments/" + enc(paymentId))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(timeout())
                    .block();
            return mapper.readTree(StringUtils.hasText(raw) ? raw : "{}");
        } catch (Exception e) {
            return mapper.createObjectNode();
        }
    }

    @Override
    public JsonNode listPaymentsByBillingKey(String billingKey, int size) {
        try {
            String raw = portoneWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/payments")
                            .queryParam("billingKey", billingKey)
                            .queryParam("size", size)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(timeout())
                    .block();
            return mapper.readTree(StringUtils.hasText(raw) ? raw : "{}");
        } catch (Exception e) {
            return mapper.createObjectNode();
        }
    }

    @Override
    public JsonNode getPaymentByOrderId(String orderId) {
        try {
            String raw = portoneWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/payments")
                            .queryParam("orderId", orderId)
                            .queryParam("size", 10)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(timeout())
                    .block();
            return mapper.readTree(StringUtils.hasText(raw) ? raw : "{}");
        } catch (Exception e) {
            return mapper.createObjectNode();
        }
    }

    // ======================== 스케줄 결제(빌링키) ========================
    // V2: POST /payments/{paymentId}/schedule  (body: { payment: BillingKeyPaymentInput, timeToPay: RFC3339 })
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
            final String useCurr = hasText(currency) ? currency : "KRW";
            final String useName = hasText(orderName) ? orderName : "Dodam Subscription";

            // payment(BillingKeyPaymentInput)
            ObjectNode payment = mapper.createObjectNode();
            ObjectNode amountNode = mapper.createObjectNode();
            amountNode.put("total", amount);
            payment.set("amount", amountNode);
            payment.put("currency", useCurr);
            payment.put("orderName", useName);
            payment.put("billingKey", billingKey);

            ObjectNode customer = mapper.createObjectNode();
            if (hasText(customerId)) customer.put("id", customerId);
            payment.set("customer", customer);

            ObjectNode body = mapper.createObjectNode();
            body.set("payment", payment);
            // RFC3339 (예: 2025-10-04T08:21:30Z)
            body.put("timeToPay", timeToPayUtc.toString());

            String raw = portoneWebClient
                    .post()
                    .uri("/payments/{paymentId}/schedule", paymentId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(body.toString()))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(timeout())
                    .doOnSubscribe(s -> log.info("[PortOne] POST https://api.portone.io/payments/{}/schedule  Authorization=PortOne ****", paymentId))
                    .block();

            return mapper.readTree(StringUtils.hasText(raw) ? raw : "{}");
        } catch (Exception e) {
            log.warn("[PortOne] scheduleByBillingKey error: {}", e.toString());
            ObjectNode err = mapper.createObjectNode();
            err.put("status", "ERROR");
            err.put("message", e.toString());
            return err;
        }
    }

    @Override
    public java.util.Map<String, Object> confirmIssueBillingKey(String billingIssueToken) {
        throw new UnsupportedOperationException("confirmIssueBillingKey is not implemented in this service.");
    }
    
    //---------- 결제 취소 --------
    @Override
    public CancelResponse cancelPayment(String paymentId,
                                        Long amount,
                                        Long taxFreeAmount,
                                        Long vatAmount,
                                        String reason) {
        try {
            Map<String,Object> body = new HashMap<>();
            if (props.getStoreId() != null) body.put("storeId", props.getStoreId());
            if (amount != null) body.put("amount", amount);
            if (taxFreeAmount != null) body.put("taxFreeAmount", taxFreeAmount);
            if (vatAmount != null) body.put("vatAmount", vatAmount);
            body.put("reason", (reason != null && !reason.isBlank()) ? reason : "user_requested");

            String raw = portoneWebClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/payments/{paymentId}/cancel").build(paymentId))
                    .header("Authorization", props.authHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(body))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode r = mapper.readTree(raw == null ? "{}" : raw);
            // v2 응답: cancellation 객체 안에 상태가 담김 (필드명은 계속 확장 가능성 있어 raw도 함께 보관)
            String status = jst(r, "cancellation.status");
            if (!hasText(status)) status = jst(r, "status");
            if (!hasText(status)) status = "UNKNOWN";

            log.info("[PortOne] cancel paymentId={} -> {}", paymentId, status);
            return new CancelResponse(status, raw);
        } catch (Exception e) {
            log.warn("[PortOne] cancel failed paymentId={}, ex={}", paymentId, e.toString());
            return new CancelResponse("ERROR", null);
        }
    }

    @Override
    public CancelSchedulesResponse cancelPaymentSchedules(String billingKey, List<String> scheduleIds) {
        try {
            Map<String,Object> body = new HashMap<>();
            if (props.getStoreId() != null) body.put("storeId", props.getStoreId());
            if (billingKey != null && !billingKey.isBlank()) body.put("billingKey", billingKey);
            if (scheduleIds != null && !scheduleIds.isEmpty()) body.put("scheduleIds", scheduleIds);

            String raw = portoneWebClient
                    .method(HttpMethod.DELETE)
                    .uri("/payment-schedules")
                    .header("Authorization", props.authHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(body))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode r = mapper.readTree(raw == null ? "{}" : raw);
            List<String> revoked = new ArrayList<>();
            JsonNode arr = r.path("revokedScheduleIds");
            if (arr.isArray()) arr.forEach(n -> revoked.add(n.asText()));

            String revokedAt = jst(r, "revokedAt");
            log.info("[PortOne] unschedule billingKey={} revoked={}", billingKey, revoked);
            return new CancelSchedulesResponse(revoked, revokedAt, raw);
        } catch (Exception e) {
            log.warn("[PortOne] unschedule failed billingKey={}, ex={}", billingKey, e.toString());
            return new CancelSchedulesResponse(List.of(), null, null);
        }
    }
}
