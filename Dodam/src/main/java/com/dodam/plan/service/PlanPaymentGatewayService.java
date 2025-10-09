package com.dodam.plan.service;

import java.util.Optional;

import com.dodam.plan.dto.PlanCardMeta;
import com.dodam.plan.dto.PlanLookupResult;
import com.dodam.plan.dto.PlanPayResult;
import com.dodam.plan.dto.PlanPaymentLookupResult;
import com.fasterxml.jackson.databind.JsonNode;

public interface PlanPaymentGatewayService {

    /** 빌링키 결제 (기존 시그니처 유지: 첫 인자는 이제 'orderId'로 해석) */
    PlanPayResult payByBillingKey(String orderId, String billingKey, long amount, String customerId);

    /** 레거시(3인자) 호환: 내부에서 orderId를 생성해 전달하는 구현에 위임할 수도 있음 */
    default PlanPayResult payByBillingKey(String billingKey, long amount, String customerId) {
        String orderId = "inv-ts-" + System.currentTimeMillis(); // 임시 (컨트롤러에서 생성해 넘기는 걸 권장)
        return payByBillingKey(orderId, billingKey, amount, customerId);
    }

    /** 세부 파라미터 버전(첫 인자는 'orderId') */
    PlanPayResult payByBillingKey(String orderId, String billingKey, long amount, String currency,
                                  String orderName, String storeId, String customerId, String channelKey);

    /** ---- 새로 추가: 표준 confirm + orderId 검색 ---- */
    /** PortOne /payments/confirm (orderId 기반) */
    JsonNode confirmBilling(String orderId, String billingKey, long amount,
                            String currency, String orderName, String customerId);

    /** GET /payments?orderId=... (size=1) */
    JsonNode findByOrderId(String orderId);

    /** 결제전문(raw JSON)에서 카드 메타 추출 */
    PlanCardMeta extractCardMeta(String rawJson);

    /** 결제 상태 안전 조회 (예외 삼키고 UNKNOWN) — paymentId 또는 orderId 모두 허용 */
    PlanLookupResult safeLookup(String anyId);

    /** 결제 단건 조회 — paymentId 또는 orderId 모두 허용 */
    PlanLookupResult lookup(String anyId);

    /* ===== 결과 타입 (레거시) ===== */
    record PayResult(String paymentId, String status, String raw) {}
    record LookupResult(String paymentId, String status, String raw) {}

    /** 결제 상태 조회 — paymentId 또는 orderId 모두 허용 */
    PlanPaymentLookupResult lookupPayment(String anyId);
    
    Optional<JsonNode> findPaymentByExactOrderId(String orderId);


    /** 인보이스 결제 시도 (동기/비동기) — 미사용 시 기본 구현 */
    default PlanPaymentLookupResult confirmInvoice(Long invoiceId, String mid) {
        return new PlanPaymentLookupResult(null, "FAILED", "NOT_IMPLEMENTED",
                org.springframework.http.HttpStatus.OK);
    }
}
