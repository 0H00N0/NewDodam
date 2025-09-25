package com.dodam.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.dodam.product.entity.ProstateEntity;

public interface ProstateRepository extends JpaRepository<ProstateEntity, Long> {
	
}