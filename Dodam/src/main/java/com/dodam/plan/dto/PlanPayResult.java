// src/main/java/com/dodam/plan/dto/PlanPayResult.java
package com.dodam.plan.dto;

public record PlanPayResult(
        boolean success,
        String paymentId,
        String receiptUrl,
        String failReason,
        String status,
        String rawJson
) {}
