// src/main/java/com/dodam/plan/dto/MySubscriptionDto.java
package com.dodam.plan.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PlanMySubscriptionDTO(
        Long pmId,
        String status,

        String planCode,
        String planName,
        Integer termMonth,
        BigDecimal amount,
        String currency,
        LocalDateTime termStart,
        LocalDateTime termEnd,
        LocalDateTime nextBillingAt,

        // ---- 다음 주기(변경 예약) 표시용 ----
        boolean hasScheduledChange,
        String nextPlanCode,
        String nextPlanName,
        Integer nextTermMonth,
        BigDecimal nextAmount,
        String nextCurrency
) {}
