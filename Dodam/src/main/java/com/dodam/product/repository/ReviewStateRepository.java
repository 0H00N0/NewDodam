package com.dodam.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.dodam.product.entity.ReviewStateEntity;

public interface ReviewStateRepository extends JpaRepository<ReviewStateEntity, Long> {
	
}

