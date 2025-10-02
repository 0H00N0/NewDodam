package com.dodam.product.dto;

import com.dodam.product.entity.CategoryEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CategoryDTO {
    private Long catenum;
    private String catename;

    // 엔티티 → DTO 변환 메서드
    public static CategoryDTO fromEntity(CategoryEntity entity) {
        return CategoryDTO.builder()
                .catenum(entity.getCatenum())
                .catename(entity.getCatename())
                .build();
    }
}