package com.dodam.plan.enums;

public final class PlanEnums{
public enum PmStatus { ACTIVE, PAUSED, CANCELED, EXPIRED, PENDING }
public enum PmBillingMode { MONTHLY, PREPAID_TERM }

public enum PiStatus { PENDING, PAID, FAILED, CANCELED, REFUNDED }
public enum PattResult { SUCCESS, FAIL }

public enum PrefType { FULL, PARTIAL, VOID, CHARGEBACK }
public enum PrefStatus { REQUESTED, APPROVED, PROCESSING, REFUNDED, REJECTED, FAILED }
public enum PrefMethod { ORIGINAL, MANUAL }
}