// com/dodam/main/dto/MainReviewBriefDTO.java
package com.dodam.main.dto;
import lombok.AllArgsConstructor; import lombok.Getter;

@Getter @AllArgsConstructor
public class MainReviewBriefDTO {
    private Long   revId;       // 리뷰 PK
    private String title;       // revtitle
    private Integer score;      // revscore
    private String createdAt;   // ISO string
    private Long   proId;       // 상품 PK(pronum)
    private String proName;     // 상품명
    private String imageUrl;    // 상품 대표 이미지
}
