// src/main/java/com/dodam/plan/service/PlanPaymentGatewayService.java
package com.dodam.plan.service;

import com.dodam.plan.dto.PlanCardMeta;
import com.dodam.plan.dto.PlanLookupResult;
import com.dodam.plan.dto.PlanPayResult;
import com.dodam.plan.dto.PlanPaymentLookupResult;

public interface PlanPaymentGatewayService {

    /** 빌링키 결제 */
    PlanPayResult payByBillingKey(String paymentId, String billingKey, long amount, String customerId);

    /** 레거시(3인자) 호환 */
    default PlanPayResult payByBillingKey(String billingKey, long amount, String customerId) {
        String pid = "pay-" + System.currentTimeMillis();
        return payByBillingKey(pid, billingKey, amount, customerId);
    }

    PlanPayResult payByBillingKey(String paymentId, String billingKey, long amount, String currency,
                                  String orderName, String storeId, String customerId, String channelKey);

    /** 결제전문(raw JSON)에서 카드 메타 추출 */
    PlanCardMeta extractCardMeta(String rawJson);

    /** 결제 상태 안전 조회 (예외 삼키고 UNKNOWN) */
    PlanLookupResult safeLookup(String paymentId);

    /** 결제 단건 조회 */
    PlanLookupResult lookup(String paymentId);

    /* ===== 결과 타입 ===== */
    record PayResult(String paymentId, String status, String raw) {}
    record LookupResult(String paymentId, String status, String raw) {}

    /** 결제 상태 조회 */
    PlanPaymentLookupResult lookupPayment(String paymentId);

    /** 인보이스 결제 시도 (동기/비동기) — 미사용 시 기본 구현 */
    default PlanPaymentLookupResult confirmInvoice(Long invoiceId, String mid) {
        return new PlanPaymentLookupResult(null, "FAILED", "NOT_IMPLEMENTED",
                org.springframework.http.HttpStatus.OK);
    }
}
