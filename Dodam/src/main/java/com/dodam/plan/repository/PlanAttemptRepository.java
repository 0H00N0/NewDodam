package com.dodam.plan.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dodam.plan.Entity.PlanAttemptEntity;

public interface PlanAttemptRepository extends JpaRepository<PlanAttemptEntity, Long> {
    List<PlanAttemptEntity> findByInvoice_PiIdOrderByPattIdDesc(Long piId);

    @Query(value = """
            SELECT t.PATTUID
              FROM PLANATTEMPT t
             WHERE t.PIID = :invoiceId
             ORDER BY t.PATTAT DESC
             FETCH FIRST 1 ROWS ONLY
            """, nativeQuery = true)
    Optional<String> findLatestPaymentUidByInvoiceId(@Param("invoiceId") Long invoiceId);
    
    List<PlanAttemptEntity> findByPild_PiId(Long piId);

}
