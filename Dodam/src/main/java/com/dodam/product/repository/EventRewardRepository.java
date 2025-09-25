package com.dodam.product.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.dodam.product.entity.EventRewardEntity;

public interface EventRewardRepository extends JpaRepository<EventRewardEntity, Long> {
	
}
