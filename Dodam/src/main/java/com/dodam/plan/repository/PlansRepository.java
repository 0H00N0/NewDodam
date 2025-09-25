package com.dodam.plan.repository;

import com.dodam.plan.Entity.PlansEntity;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlansRepository extends JpaRepository<PlansEntity, Long>{
	Optional<PlansEntity> findByPlanCode(String planCode);
	List<PlansEntity> findByPlanActiveTrue();
	Optional<PlansEntity> findByPlanCodeIgnoreCase(String planCode);
	Optional<PlansEntity> findByPlanCodeEqualsIgnoreCase(String planCode);
}
