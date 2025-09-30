package com.dodam.plan.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dodam.plan.Entity.PlanRefundEntity;
import com.dodam.plan.enums.PlanEnums;

public interface PlanRefundRepository extends JpaRepository<PlanRefundEntity, Long> {
	  List<PlanRefundEntity> findByInvoice_PiId(Long piId);
	  
	  List<PlanRefundEntity> findByPrefStat(PlanEnums.PrefStatus status);

	}
