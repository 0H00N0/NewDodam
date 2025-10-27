package com.dodam.product.repository;

import com.dodam.product.entity.ProductEntity;
import com.dodam.product.entity.ProductImageEntity;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImageEntity, Long> {
    void deleteByProduct(ProductEntity product);
    
    // ✅ 가장 먼저 등록된(혹은 정렬이 빠른) 이미지 한 장
    Optional<ProductImageEntity> findFirstByProductOrderByProimageorderAsc(ProductEntity product);
}
