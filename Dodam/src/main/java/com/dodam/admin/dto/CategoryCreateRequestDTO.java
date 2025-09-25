package com.dodam.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryCreateRequestDTO { //카테고리 등록 시 사용
    
    @NotBlank(message = "카테고리 이름은 필수입니다.")
    private String categoryName;
}