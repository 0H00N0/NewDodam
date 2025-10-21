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

    @Query(value = """
        SELECT t.PATTURL
          FROM PLANATTEMPT t
         WHERE t.PIID = :invoiceId
           AND t.PATTRESULT = 'SUCCESS'
           AND t.PATTURL IS NOT NULL
         ORDER BY t.PATTAT DESC
         FETCH FIRST 1 ROWS ONLY
    """, nativeQuery = true)
    Optional<String> findLatestReceiptUrlByInvoiceId(@Param("invoiceId") Long invoiceId);

    // 🔧 공백/대소문자 차이를 무시하고 매칭
    @Query(value = """
        SELECT t.PATTURL
          FROM PLANATTEMPT t
         WHERE LOWER(TRIM(t.PATTUID)) = LOWER(TRIM(:paymentUid))
           AND t.PATTRESULT = 'SUCCESS'
           AND t.PATTURL IS NOT NULL
         ORDER BY t.PATTAT DESC
         FETCH FIRST 1 ROWS ONLY
    """, nativeQuery = true)
    Optional<String> findLatestReceiptUrlByPaymentUid(@Param("paymentUid") String paymentUid);
    
 // ✅ paymentUid(PATTUID)로 인보이스ID 역추적 (대소문자/공백 무시)
    @Query(value = """
        SELECT t.PIID
          FROM PLANATTEMPT t
         WHERE LOWER(TRIM(t.PATTUID)) = LOWER(TRIM(:paymentUid))
         ORDER BY t.PATTAT DESC
         FETCH FIRST 1 ROWS ONLY
    """, nativeQuery = true)
    Optional<Long> findInvoiceIdByPaymentUid(@Param("paymentUid") String paymentUid);
}
