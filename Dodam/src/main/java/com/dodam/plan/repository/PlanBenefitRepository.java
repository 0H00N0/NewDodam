package com.dodam.plan.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dodam.plan.Entity.PlanBenefitEntity;
import com.dodam.plan.Entity.PlansEntity;

public interface PlanBenefitRepository extends JpaRepository<PlanBenefitEntity, Long>{
	Optional<PlanBenefitEntity> findFirstByPlan(PlansEntity plan);
	List<PlanBenefitEntity> findByPlanIn(List<PlansEntity> plans);
	// PlanBenefitRepository
	List<PlanBenefitEntity> findByPlan_PlanId(Long planId);
	// PlanBenefitRepository
	void deleteByPlan_PlanId(Long planId);
	// 특정 플랜에 속한 혜택 정보 전체 조회
    List<PlanBenefitEntity> findByPlan(PlansEntity plan);

    // 특정 플랜의 혜택 정보 전체 삭제
    void deleteByPlan(PlansEntity plan);
	
}

