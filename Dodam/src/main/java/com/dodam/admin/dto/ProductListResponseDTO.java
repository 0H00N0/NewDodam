package com.dodam.admin.dto;

import com.dodam.product.entity.ProductEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class ProductListResponseDTO {

    private Long pronum;
    private String proname;
    private BigDecimal proborrow;   // ✅ proprice 제거 → proborrow로 변경
    private String probrand;
    private String categoryName;
    private String productGrade; // S, A, B, C 등급
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime procre; // 등록일

    public ProductListResponseDTO(ProductEntity product) {
        this.pronum = product.getPronum();
        this.proname = product.getProname();
        this.proborrow = product.getProborrow();  // ✅ 대여 가격 매핑
        this.probrand = product.getProbrand();

        if (product.getCategory() != null) {
            this.categoryName = product.getCategory().getCatename();
        }
        if (product.getProstate() != null) {
            this.productGrade = product.getProstate().getPrograde();
        }
        this.procre = product.getProcre();
    }
}
