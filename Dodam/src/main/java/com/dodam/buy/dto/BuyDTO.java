package com.dodam.buy.dto;

import lombok.*;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class BuyDTO {
    private Long buynum;
    private Long mnum;
    private Long pronum;
    private Long catenum;
    private Long prosnum;
}