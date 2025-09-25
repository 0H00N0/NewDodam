package com.dodam.admin.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class DeliverymanRequestDTO {
	
	@NotNull
	private Long pronum;
	
	@NotNull
	private Long mnum;
	
	@Min(0)
	private Integer dayoff;
	
	@DecimalMin(value = "0.00")
	private BigDecimal delcost;
	
	@Size(max = 200)
	private String location;
	
}
