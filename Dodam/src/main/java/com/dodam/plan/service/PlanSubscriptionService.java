package com.dodam.plan.service;

import com.dodam.plan.Entity.PlanInvoiceEntity;
import com.dodam.plan.dto.PlanSubscriptionStartReq;

import java.util.Map;

public interface PlanSubscriptionService {

    /** 인보이스가 결제 확정되었을 때 구독/인보이스 상태를 반영 */
    void activateInvoice(PlanInvoiceEntity invoice, int months);

    /**
     * (빌링키 즉시결제 → 폴링으로 확정)까지 한 번에 처리
     * @param invoiceId 인보이스 ID (PENDING 상태)
     * @param mid       회원 ID
     * @param termMonths 1,3,6,12 등
     */
    Map<String, Object> chargeByBillingKeyAndConfirm(Long invoiceId, String mid, int termMonths);

    /** 플랜코드/개월수로 인보이스 생성(or 재사용) 후 결제+확정 */
    Map<String, Object> chargeAndConfirm(String mid, PlanSubscriptionStartReq req);

    /** ✅ 다음 결제 예약 해지 (현재 기간 유지, 다음 자동결제만 중단) — (이전 API, 계속 유지) */
    CancelNextResult cancelNextRenewal(String mid, String reason);

    /** ✅ 기간말 해지 예약: 지금은 유지하고, 현재 주기 끝나면 해지 */
    void scheduleCancelAtPeriodEnd(Long pmId, String mid);

    /** ✅ 기간말 해지 예약 취소: 해지 예약을 되돌려서 계속 이용 */
    void revertCancelAtPeriodEnd(Long pmId, String mid);

    /** ✅ 주기 종료 시 해지 완료 처리 (스케줄러/배치/로그인 시 훅) */
    void finalizeCancelIfDue(Long pmId);

    /** 해지 결과 */
    record CancelNextResult(
            boolean autoRenewDisabled,
            boolean upcomingInvoiceCanceled,
            boolean pgScheduleCanceled,
            String message
    ) {}
}
