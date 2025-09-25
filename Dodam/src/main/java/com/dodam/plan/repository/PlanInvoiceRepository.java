package com.dodam.plan.repository;

import com.dodam.plan.Entity.PlanInvoiceEntity;
import com.dodam.plan.enums.PlanEnums;
import com.dodam.plan.enums.PlanEnums.PiStatus;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

public interface PlanInvoiceRepository extends JpaRepository<PlanInvoiceEntity, Long> {

	Optional<PlanInvoiceEntity> findByPiUid(String piUid);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select i from PlanInvoiceEntity i where i.piId = :piId")
	Optional<PlanInvoiceEntity> findForUpdate(@Param("piId") Long piId);

	/** PAID 처리 + piUid 비어있으면 세팅 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update PlanInvoiceEntity i
           set i.piStat = :paid,
               i.piPaid = :paidAt,
               i.piUid  = coalesce(i.piUid, :uid)
         where i.piId = :piId
    """)
    int markPaidAndSetUidIfEmpty(@Param("piId") Long piId,
                                 @Param("uid") String uid,
                                 @Param("paidAt") LocalDateTime paidAt,
                                 @Param("paid") PlanEnums.PiStatus paid);

    default int markPaidAndSetUidIfEmpty(Long piId, String uid, LocalDateTime paidAt) {
        return markPaidAndSetUidIfEmpty(piId, uid, paidAt, PlanEnums.PiStatus.PAID);
    }
	/** 컨트롤러에서 호출하는 메서드: “빈 구현”으로 제공해 컴파일/런타임 안전 */
    default Optional<PlanInvoiceEntity> findRecentPendingSameAmount(
            String mid,
            PlanEnums.PiStatus status,
            BigDecimal amount,
            String currency,
            LocalDateTime from,
            LocalDateTime to
    ) {
        return Optional.empty();
    }
}
