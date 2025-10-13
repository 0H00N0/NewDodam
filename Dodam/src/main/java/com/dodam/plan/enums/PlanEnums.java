package com.dodam.plan.enums;

public final class PlanEnums{
public enum PmStatus { ACTIVE, PAUSED, CANCELED, EXPIRED, PENDING, CANCEL_SCHEDULED }
public enum PmBillingMode { MONTHLY, PREPAID_TERM }

public enum PiStatus { PENDING, PAID, FAILED, CANCELED, REFUNDED, READY }
public enum PattResult {
    SUCCESS,
    FAIL,
    PENDING,     // 결제 요청은 됐지만 웹훅/승인 대기
    ACCEPTED,    // PG 응답 수신은 되었으나 미확정
    TIMEOUT      // API 통신 실패/지연
}

public enum PrefType { FULL, PARTIAL, VOID, CHARGEBACK }
public enum PrefStatus { REQUESTED, APPROVED, PROCESSING, REFUNDED, REJECTED, FAILED }
public enum PrefMethod { ORIGINAL, MANUAL }
}