package com.dodam.plan.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class PlanCardMeta {
    private String billingKey; // 어떤 billingKey인지
    private String brand;      // 카드 브랜드
    private String bin;        // 카드 BIN
    private String last4;      // 끝 4자리
    private String pg;         // PG 제공자 (tosspayments 등)
    private boolean issued;    // 빌링키 발급 성공 여부
    private String customerId; // 고객 식별자

    // 하위호환: (brand, bin, last4, issuerName) 생성자
    public PlanCardMeta(String brand, String bin, String last4, String issuerName) {
        this.brand = brand;
        this.bin = bin;
        this.last4 = last4;
        this.pg = issuerName;
        this.issued = true;
    }

    public PlanCardMeta(String customerId, String brand, String bin, String last4, String pg) {
        this.brand = brand;
        this.bin = bin;
        this.last4 = last4;
        this.pg = pg;
        this.issued = true;
        this.customerId = customerId;
    }
}
