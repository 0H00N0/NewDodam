package com.dodam.plan.dto;

import org.springframework.http.HttpStatus;

public record PlanPaymentLookupResult(
        String paymentId,
        String status,   // "PAID" | "FAILED" | "CANCELLED" | "PENDING" | "UNKNOWN"
        String message,
        HttpStatus httpStatus
) {
    public static PlanPaymentLookupResult of(String pid, String status, String message) {
        return new PlanPaymentLookupResult(pid, status, message, HttpStatus.OK);
    }
}
