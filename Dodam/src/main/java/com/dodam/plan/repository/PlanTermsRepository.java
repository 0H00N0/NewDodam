package com.dodam.plan.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dodam.plan.Entity.PlanTermsEntity;

public interface PlanTermsRepository extends JpaRepository<PlanTermsEntity, Long>{
	Optional<PlanTermsEntity> findByPtermMonth(Integer ptermMonth);
	
	@Query("select t.ptermMonth from PlanTermsEntity t where t.ptermId = :termId")
    Integer findMonthsByTermId(@Param("termId") Long termId);
}
