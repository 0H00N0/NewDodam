package com.dodam.cart.dto;

import lombok.*;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class CartDTO {
    private Long cartnum;
    private Long mnum;
    private Long pronum;
    private Long catenum;
    private Long resernum;
}