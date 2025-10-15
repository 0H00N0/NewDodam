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
        LocalDateTime nextBillingAt,   // = 다음 주기 시작일(백엔드 산정 기준), 하위 호환

        // ---- 다음 주기 이용기간(서버 계산 결과) ----
        LocalDateTime nextTermStart,   // = 보통 nextBillingAt과 동일
        LocalDateTime nextTermEnd,     // = nextTermStart + months

        // ---- 다음 주기(변경 예약) 표시용 ----
        boolean hasScheduledChange,
        String nextPlanCode,
        String nextPlanName,
        Integer nextTermMonth,
        BigDecimal nextAmount,
        String nextCurrency
) {}
