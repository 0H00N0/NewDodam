package com.dodam.product.dto;

import lombok.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductDTO {

    // PK
    private Long pronum;

    // 기본 컬럼
    private String proname;
    private String prodetail;
    private BigDecimal proprice;
    private BigDecimal proborrow;
    private String probrand;
    private String promade;
    private Integer proage;
    private String procertif;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate prodate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime procre;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime proupdate;

    // FK(숫자)
    private Long catenum;   // category PK
    private Long prosnum;   // prostate PK
    private Long resernum;  // 예약 FK(스칼라)
    private Long ctnum;     // 쿠폰종류 FK(스칼라)

    // 이미지
    private List<ProductImageDTO> images;
}
