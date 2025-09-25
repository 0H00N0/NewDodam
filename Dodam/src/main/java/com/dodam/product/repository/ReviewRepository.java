package com.dodam.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.dodam.product.entity.ReviewEntity;

public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {
	
}
