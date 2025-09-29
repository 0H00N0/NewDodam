package com.dodam.main.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MainNewProductByNameDTO {
    private String name;     // proname
    private Long   proId;    // pronum
    private String imageUrl; // productimage.prourl (대표 1장)
    private Long   price;    // proborrow
    private String procre;   // ISO 문자열 (원하면 LocalDateTime으로 매핑)
}
