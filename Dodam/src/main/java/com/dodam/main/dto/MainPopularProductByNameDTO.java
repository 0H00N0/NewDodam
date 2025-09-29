package com.dodam.main.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MainPopularProductByNameDTO {
    private String name;     // proname
    private Long   proId;    // 대표 pronum
    private String imageUrl; // productimage.prourl
    private Long   price;    // proborrow
    private Long   rentCount;// 해당 이름의 전체 대여 합계
}
