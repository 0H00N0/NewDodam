package com.dodam.product.controller;

import com.dodam.product.dto.CategoryDTO;
import com.dodam.product.entity.CategoryEntity;
import com.dodam.product.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepository categoryRepository;

    // 카테고리 이름으로 카테고리 정보 조회
    @GetMapping("/{categoryName}")
    public CategoryDTO getCategoryByName(@PathVariable String categoryName) {
        CategoryEntity category = categoryRepository.findByCatename(categoryName)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "카테고리 없음: " + categoryName));
        return CategoryDTO.fromEntity(category);
    }
}