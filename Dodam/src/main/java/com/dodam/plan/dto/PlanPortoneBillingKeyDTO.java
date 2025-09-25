package com.dodam.plan.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PlanPortoneBillingKeyDTO {
    private String billingKey;
    private Customer customer;

    @Getter @Setter
    public static class Customer {
        private String id;
        private String name;
    }
}
