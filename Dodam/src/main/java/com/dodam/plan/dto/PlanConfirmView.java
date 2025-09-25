package com.dodam.plan.dto;

public record PlanConfirmView(
    Long invoiceId,
    Long amount,
    String mid,
    String customerId,
    Long attemptsCount
) {}
