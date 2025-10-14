package com.dodam.plan.dto;

import lombok.Data;

@Data
public class PlanChangeReq {
    private Long pmId;        // 대상 구독
    private String planCode;  // 새 플랜 코드 (예: BASIC / STANDARD ...)
    private Integer months;   // 새 약정 개월수 (1=월간, 3/6/12 등)
    private Long priceId;     // (선택) 특정 가격 ID가 확정돼 있다면 사용
}
