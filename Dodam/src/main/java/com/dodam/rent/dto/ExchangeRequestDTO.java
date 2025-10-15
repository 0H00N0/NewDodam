package com.dodam.rent.dto;
import lombok.Getter;

@Getter
public class ExchangeRequestDTO {
    private Long newPronum;  // 교환할 상품 번호
    private String reason;   // 선택
}
