package com.dodam.cart.dto;

import java.math.BigDecimal;
import lombok.*;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class CartItemViewDTO {
    private Long cartnum;
    private Long pronum;
    private String proname;
    private BigDecimal price;      // 표시용 현재가
    private String thumbnail;      // 썸네일 경로(없으면 null)
    private Integer qty;           // 현재 스키마엔 수량 컬럼이 없으므로 1 고정
}
