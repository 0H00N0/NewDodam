// src/main/java/com/dodam/plan/dto/PlanPaymentRegisterReq.java
package com.dodam.plan.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanPaymentRegisterReq {
    // 프로필/고객 식별용
    private String customerId;

    // 빌링키 및 카드 메타
    @NotBlank(message = "billingKey는 필수입니다.")
    private String billingKey;
    private String pg;     // 예: TOSSPAYMENTS, KCP 등
    private String brand;  // 예: 삼성, 현대, BC...
    private String bin;    // 6~8자리
    private String last4;  // 끝 4자리

    // 원본 JSON (있으면 저장/분석)
    @NotBlank(message = "rawJson은 필수입니다.")
    private String rawJson;

    // (선택) 리다이렉트/조회 관련 보조 필드
    private String txId;
    private String transactionType;

    // ★ 기존 코드 호환용 7-인자 생성자 (customerId, billingKey, pg, brand, bin, last4, rawJson)
    public PlanPaymentRegisterReq(String customerId, String billingKey, String pg,
                                  String brand, String bin, String last4, String rawJson) {
        this.customerId = customerId;
        this.billingKey = billingKey;
        this.pg = pg;
        this.brand = brand;
        this.bin = bin;
        this.last4 = last4;
        this.rawJson = rawJson;
    }
}
