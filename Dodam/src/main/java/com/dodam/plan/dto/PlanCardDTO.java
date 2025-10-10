package com.dodam.plan.dto;

import com.dodam.plan.Entity.PlanPaymentEntity;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PlanCardDTO {
    private Long id;
    private String brand;
    private String last4;
    private String pg;
    private String billingKey;

    public static PlanCardDTO from(PlanPaymentEntity e) {
        return new PlanCardDTO(
                e.getPayId(),
                e.getPayBrand(),
                e.getPayLast4(),
                e.getPayPg(),
                e.getPayKey()
        );
    }
}
