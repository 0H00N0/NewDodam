package com.dodam.plan.dto;

import java.math.BigDecimal;

import com.dodam.plan.Entity.PlanPriceEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanPriceDTO {
    private Long ppriceId;
    private Integer termMonth;   // 단수형(기존)
    private Integer months;      // 프론트 호환
    private String bilMode;
    private BigDecimal amount;
    private Long amountInt;      // 프론트 호환(Long)
    private String currency;
    private Boolean active;

    // (옵션) 사전 계산된 할인율을 내려줄 수 있게 필드만 추가
    private Integer discountRate;

    public static PlanPriceDTO from(PlanPriceEntity e) {
        Integer m = (e.getPterm() != null) ? e.getPterm().getPtermMonth() : null;
        BigDecimal amt = e.getPpriceAmount();

        return PlanPriceDTO.builder()
                .ppriceId(e.getPpriceId())
                .amount(amt)
                .amountInt(amt != null ? amt.longValue() : null)
                .currency(e.getPpriceCurr())
                .bilMode(e.getPpriceBilMode())
                .termMonth(m)
                .months(m)
                .active(Boolean.TRUE.equals(e.getPpriceActive()))
                .build();
    }
}
