package com.dodam.admin.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RefundRequestDTO {
    private Long piId;             // 결제 ID (필수)
    private BigDecimal amount;     // 환불 금액 (선택)
    private String type;           // FULL, PARTIAL, VOID, CHARGEBACK (필수)
    private String reason;         // 사유 (선택)

    // ▼ 선택 필드 (있으면 더 풍부한 관리 가능)
    private Long attemptId;        // 실패/성공 attempt와 연결하고 싶을 때
    private String method;         // ORIGINAL or MANUAL (미지정 시 ORIGINAL로 처리)
}

