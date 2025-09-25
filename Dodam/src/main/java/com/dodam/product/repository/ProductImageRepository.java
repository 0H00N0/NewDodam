package com.dodam.product.repository;

import com.dodam.product.entity.ProductEntity;
import com.dodam.product.entity.ProductImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImageEntity, Long> {
    void deleteByProduct(ProductEntity product);
}
