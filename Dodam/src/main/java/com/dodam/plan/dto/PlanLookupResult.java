package com.dodam.plan.dto;

public record PlanLookupResult(
        boolean success,
        String paymentId,
        String status,
        String rawJson
) {}
