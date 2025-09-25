package com.dodam.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.dodam.product.entity.CategoryEntity;

public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {
	
}
