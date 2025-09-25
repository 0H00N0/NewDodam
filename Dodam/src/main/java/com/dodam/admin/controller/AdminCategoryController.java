package com.dodam.admin.controller;

import com.dodam.admin.dto.CategoryCreateRequestDTO;
import com.dodam.admin.dto.CategoryResponseDTO;
import com.dodam.admin.dto.CategoryUpdateRequestDTO;
import com.dodam.admin.service.AdminCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/categories") // 기본 경로를 /categories로 설정
@RequiredArgsConstructor
public class AdminCategoryController {

    private final AdminCategoryService adminCategoryService;

    // CREATE: 카테고리 생성 (POST /api/v1/admin/categories)
    @PostMapping
    public ResponseEntity<CategoryResponseDTO> createCategory(@Valid @RequestBody CategoryCreateRequestDTO requestDTO) {
        CategoryResponseDTO createdCategory = adminCategoryService.createCategory(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCategory);
    }

    // READ: 모든 카테고리 조회 (GET /api/v1/admin/categories)
    @GetMapping
    public ResponseEntity<List<CategoryResponseDTO>> getAllCategories() {
        List<CategoryResponseDTO> categories = adminCategoryService.findAllCategories();
        return ResponseEntity.ok(categories);
    }

    // UPDATE: 특정 카테고리 수정 (PUT /api/v1/admin/categories/{categoryId})
    @PutMapping("/{categoryId}")
    public ResponseEntity<CategoryResponseDTO> updateCategory(@PathVariable Long categoryId, @Valid @RequestBody CategoryUpdateRequestDTO requestDTO) {
        CategoryResponseDTO updatedCategory = adminCategoryService.updateCategory(categoryId, requestDTO);
        return ResponseEntity.ok(updatedCategory);
    }

    // DELETE: 특정 카테고리 삭제 (DELETE /api/v1/admin/categories/{categoryId})
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long categoryId) {
        adminCategoryService.deleteCategory(categoryId);
        return ResponseEntity.noContent().build(); // 성공적으로 삭제되었으나 본문에 데이터 없음을 의미
    }
}