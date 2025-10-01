package com.dodam.main.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MainProductBasicDTO {
    private Long   proId;     // PRONUM
    private String name;      // PRONAME
    private Long   price;     // PROBORROW
    private String procre;    // 생성일 (문자열)
    private String imageUrl;  // 대표 이미지
}
