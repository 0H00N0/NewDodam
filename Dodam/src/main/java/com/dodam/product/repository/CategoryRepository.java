package com.dodam.product.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.dodam.product.entity.CategoryEntity;

public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {
	
	Optional<CategoryEntity> findByCatename(String catename);
	
}
