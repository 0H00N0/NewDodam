package com.dodam.admin.dto;

import com.dodam.product.entity.ProductEntity;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class ProductListResponseDTO {

    private Long pronum;
    private String proname;
    private BigDecimal proprice;
    private String probrand;
    private String categoryName;
    private String productGrade; // S, A, B, C 등급
    private LocalDateTime procre; // 등록일

    public ProductListResponseDTO(ProductEntity product) {
        this.pronum = product.getPronum();
        this.proname = product.getProname();
        this.proprice = product.getProprice();
        this.probrand = product.getProbrand();
        // Null 체크를 통해 안전하게 데이터 접근
        if (product.getCategory() != null) {
            this.categoryName = product.getCategory().getCatename();
        }
        if (product.getProstate() != null) {
            this.productGrade = product.getProstate().getPrograde();
        }
        this.procre = product.getProcre();
    }
}