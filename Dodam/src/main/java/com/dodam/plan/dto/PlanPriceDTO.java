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
	private Integer termMonth;
	private String bilMode;
	private BigDecimal amount;
	private String currency;
	private Boolean active;
	
	public static PlanPriceDTO from(PlanPriceEntity e) {
	    return PlanPriceDTO.builder()
	        .ppriceId(e.getPpriceId())
	        .amount(e.getPpriceAmount())
	        .currency(e.getPpriceCurr())
	        .bilMode(e.getPpriceBilMode())
	        .termMonth(e.getPterm() != null ? e.getPterm().getPtermMonth() : null) // ✅ 단수형
	        .build();
	}

}
