package com.dodam.plan.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface PlanPortoneClientService {

    Map<String, Object> confirmIssueBillingKey(String billingIssueToken);

    record ConfirmRequest(
            String paymentId,
            String billingKey,
            long amountValue,
            String currency,
            String customerId,
            String orderName,
            boolean isTest
    ) {}

    record ConfirmResponse(String id, String status, String raw) {}
    record LookupResponse(String id, String status, String raw) {}

    ConfirmResponse confirmByBillingKey(ConfirmRequest req);

    ConfirmResponse confirmByOrderId(
            String orderId,
            String billingKey,
            long amount,
            String currency,
            String customerId,
            String orderName
    );

    JsonNode scheduleByBillingKey(
            String paymentId,
            String billingKey,
            long amount,
            String currency,
            String customerId,
            String orderName,
            Instant timeToPayUtc
    );

    LookupResponse lookupPayment(String paymentId);

    JsonNode getPayment(String paymentId);
    JsonNode listPaymentsByBillingKey(String billingKey, int size);
    JsonNode getPaymentByOrderId(String orderId);
    JsonNode findPaymentByOrderId(String orderId);
    Optional<JsonNode> findPaymentByExactOrderId(String orderId);
    
    /** 결제 취소(부분/전액) */
    CancelResponse cancelPayment(String paymentId,
                                 Long amount,
                                 Long taxFreeAmount,
                                 Long vatAmount,
                                 String reason);

    /** 결제 예약 해지(billingKey 기준 또는 scheduleIds 기준) */
    CancelSchedulesResponse cancelPaymentSchedules(String billingKey, List<String> scheduleIds);

    record CancelResponse(String status, String raw) {}
    record CancelSchedulesResponse(List<String> revokedScheduleIds, String revokedAt, String raw) {}

    enum PaymentStatus { PENDING, PAID, SUCCEEDED, FAILED, CANCELED, ERROR, NOT_FOUND, UNKNOWN }

    /** 안전 조회(실패해도 NPE 방지) */
    default LookupResponse safeLookup(String id) {
        try {
            if (id == null || id.isBlank()) return new LookupResponse(null, "NOT_FOUND", null);
            LookupResponse r = lookupPayment(id);
            return (r == null) ? new LookupResponse(null, "NOT_FOUND", null) : r;
        } catch (Exception e) {
            return new LookupResponse(null, "ERROR", null);
        }
    }
}
