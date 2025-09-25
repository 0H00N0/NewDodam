package com.dodam.plan.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dodam.plan.Entity.PlanBenefitEntity;
import com.dodam.plan.Entity.PlansEntity;

public interface PlanBenefitRepository extends JpaRepository<PlanBenefitEntity, Long>{
	Optional<PlanBenefitEntity> findFirstByPlan(PlansEntity plan);
	List<PlanBenefitEntity> findByPlanIn(List<PlansEntity> plans);
}
