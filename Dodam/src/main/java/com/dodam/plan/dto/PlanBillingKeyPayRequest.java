package com.dodam.plan.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlanBillingKeyPayRequest {
    private Long amount;
    private String currency;
    private String storeId;
    private String billingKey;
    private String orderName;
    private String customerId; // 선택사항
}
