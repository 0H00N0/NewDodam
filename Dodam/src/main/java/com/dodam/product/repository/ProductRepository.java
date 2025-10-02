package com.dodam.product.repository;

import com.dodam.product.entity.CategoryEntity;
import com.dodam.product.entity.ProductEntity;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductRepository extends JpaRepository<ProductEntity, Long>,
                                          JpaSpecificationExecutor<ProductEntity> {
	 List<ProductEntity> findByCategory(CategoryEntity category);
}
