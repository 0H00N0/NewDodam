package com.dodam.product.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductImageDTO {
    private Long proimagenum;
    private Integer proimageorder;
    private String prourl;           // 미리보기
    private String prodetailimage;   // 상세
    private Long catenum;            // 소속 카테고리
    private Long pronum;             // (선택) 필요 시 사용
}
