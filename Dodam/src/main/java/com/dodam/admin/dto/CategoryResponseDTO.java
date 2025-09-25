package com.dodam.admin.dto;

import com.dodam.product.entity.CategoryEntity;
import lombok.Getter;

@Getter
public class CategoryResponseDTO { //카테고리 조회 시 사용
    private Long categoryId;
    private String categoryName;

    public CategoryResponseDTO(CategoryEntity category) {
        this.categoryId = category.getCatenum();
        this.categoryName = category.getCatename();
    }
}