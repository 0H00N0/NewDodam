// src/main/java/com/dodam/plan/dto/PlanSubscriptionStartReq.java
package com.dodam.plan.dto;

import lombok.Data;

@Data
public class PlanSubscriptionStartReq {
    private String planCode;   // 같은 플랜 중복방지
    private Integer months;    // 1,3,6,12 ...
    private Long payId;        // ✅ 사용자가 고른 결제수단(PlanPayment.payId). 있으면 이걸 우선 사용
    private String billingKey; // ✅ payId 대신 빌링키로도 지정 가능
}
