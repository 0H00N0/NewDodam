package com.dodam.main.repository;

import com.dodam.product.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MainProductSearchRepository
        extends JpaRepository<ProductEntity, Long>, JpaSpecificationExecutor<ProductEntity> {
}
