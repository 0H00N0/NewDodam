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
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final AdminCategoryService adminCategoryService;

    // CREATE: 카테고리 생성 (POST /admin/categories)
    @PostMapping
    public ResponseEntity<CategoryResponseDTO> createCategory(
            @Valid @RequestBody CategoryCreateRequestDTO requestDTO) {
        CategoryResponseDTO createdCategory = adminCategoryService.createCategory(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCategory);
    }

    // READ: 모든 카테고리 조회 (GET /admin/categories)
    @GetMapping
    public ResponseEntity<List<CategoryResponseDTO>> getAllCategories() {
        List<CategoryResponseDTO> categories = adminCategoryService.findAllCategories();
        return ResponseEntity.ok(categories);
    }

    // UPDATE: 특정 카테고리 수정 (PUT /admin/categories/{categoryId})
    @PutMapping("/{categoryId}")
    public ResponseEntity<CategoryResponseDTO> updateCategory(
            @PathVariable("categoryId") Long categoryId,  // ✅ 이름 명시
            @Valid @RequestBody CategoryUpdateRequestDTO requestDTO) {
        CategoryResponseDTO updatedCategory = adminCategoryService.updateCategory(categoryId, requestDTO);
        return ResponseEntity.ok(updatedCategory);
    }

    // DELETE: 특정 카테고리 삭제 (DELETE /admin/categories/{categoryId})
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable("categoryId") Long categoryId) {  // ✅ 이름 명시
        adminCategoryService.deleteCategory(categoryId);
        return ResponseEntity.noContent().build();
    }
}