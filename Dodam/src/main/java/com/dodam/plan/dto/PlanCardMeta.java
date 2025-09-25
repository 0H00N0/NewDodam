// src/main/java/com/dodam/plan/dto/PlanCardMeta.java
package com.dodam.plan.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor
@AllArgsConstructor
public class PlanCardMeta {
	private String billingKey; // ★ 추가: 이 카드가 어떤 billingKey 인지
    private String brand;      // 카드 브랜드
    private String bin;        // 카드 BIN
    private String last4;      // 끝 4자리
    private String pg;         // PG 제공자 (tosspayments 등)
    private boolean issued;    // 빌링키 발급 성공 여부
    private String customerId; // (선택) 고객 식별자

    // 하위호환: (brand, bin, last4, issuerName) 시그니처 대응
    public PlanCardMeta(String brand, String bin, String last4, String issuerName) {
        this.brand = brand;
        this.bin = bin;
        this.last4 = last4;
        this.pg = issuerName; // 과거 issuerName을 pg로 흡수
        this.issued = true;
    }
    
    public PlanCardMeta(String customerId, String brand, String bin, String last4, String pg) {
        this.brand = brand;
        this.bin = bin;
        this.last4 = last4;
        this.pg = pg;
        this.issued = true;      // confirm에서 만든 메타는 보통 발급 성공 컨텍스트
        this.customerId = customerId;
    }
}
