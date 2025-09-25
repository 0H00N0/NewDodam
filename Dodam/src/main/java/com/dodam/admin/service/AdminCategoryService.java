package com.dodam.admin.service;

import com.dodam.admin.dto.CategoryCreateRequestDTO;
import com.dodam.admin.dto.CategoryResponseDTO;
import com.dodam.admin.dto.CategoryUpdateRequestDTO;
import com.dodam.product.entity.CategoryEntity;
import com.dodam.product.repository.CategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminCategoryService {

    private final CategoryRepository categoryRepository;

    // CREATE
    @Transactional
    public CategoryResponseDTO createCategory(CategoryCreateRequestDTO requestDTO) {
        CategoryEntity newCategory = CategoryEntity.builder()
                .catename(requestDTO.getCategoryName())
                .build();
        CategoryEntity savedCategory = categoryRepository.save(newCategory);
        return new CategoryResponseDTO(savedCategory);
    }

    // READ (All)
    @Transactional(readOnly = true)
    public List<CategoryResponseDTO> findAllCategories() {
        return categoryRepository.findAll().stream()
                .map(CategoryResponseDTO::new)
                .collect(Collectors.toList());
    }

    // UPDATE
    @Transactional
    public CategoryResponseDTO updateCategory(Long categoryId, CategoryUpdateRequestDTO requestDTO) {
        CategoryEntity category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("해당 ID의 카테고리를 찾을 수 없습니다: " + categoryId));
        
        category.setCatename(requestDTO.getCategoryName());
        // JpaRepository의 save는 id가 있으면 update, 없으면 insert를 수행합니다.
        CategoryEntity updatedCategory = categoryRepository.save(category); 
        return new CategoryResponseDTO(updatedCategory);
    }

    // DELETE
    @Transactional
    public void deleteCategory(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new EntityNotFoundException("해당 ID의 카테고리를 찾을 수 없습니다: " + categoryId);
        }
        categoryRepository.deleteById(categoryId);
    }
}