// src/main/java/com/dodam/plan/repository/PlanInvoiceRepository.java
package com.dodam.plan.repository;

import com.dodam.plan.Entity.PlanInvoiceEntity;
import com.dodam.plan.enums.PlanEnums;
import com.dodam.plan.enums.PlanEnums.PiStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PlanInvoiceRepository extends JpaRepository<PlanInvoiceEntity, Long> {

    Optional<PlanInvoiceEntity> findByPiUid(String piUid);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from PlanInvoiceEntity i where i.piId = :piId")
    Optional<PlanInvoiceEntity> findForUpdate(@Param("piId") Long piId);
    
    Optional<PlanInvoiceEntity> findTopByPlanMember_PmIdOrderByPiIdDesc(Long pmId);


    /** PAID 처리 + piUid 비어있으면 세팅 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update PlanInvoiceEntity i
           set i.piStat = com.dodam.plan.enums.PlanEnums$PiStatus.PAID,
               i.piPaid = coalesce(i.piPaid, :paidAt),
               i.piUid  = case when (i.piUid is null or i.piUid = '') then :uid else i.piUid end
         where i.piId = :piId
    """)
    int markPaidAndSetUidIfEmpty(@Param("piId") Long piId,
                                 @Param("uid") String uid,
                                 @Param("paidAt") LocalDateTime paidAt);

   
    @Query("""
            select i from PlanInvoiceEntity i
             where i.planMember.member.mid = :mid
               and i.piStat = :status
               and i.piAmount = :amount
               and i.piCurr = :currency
               and i.piStart between :from and :to
            order by i.piId desc
            """)
    Optional<PlanInvoiceEntity> findRecentPendingSameAmount(@Param("mid") String mid,
                                                            @Param("status") PlanEnums.PiStatus status,
                                                            @Param("amount") BigDecimal amount,
                                                            @Param("currency") String currency,
                                                            @Param("from") LocalDateTime from,
                                                            @Param("to") LocalDateTime to);

    /** 회원의 모든 인보이스 조회 */
    List<PlanInvoiceEntity> findAllByPlanMember_Member_Mnum(Long mnum);

    /* ===== (추가) 보조 매칭: 금액+시간창 심플 버전 ===== */
    @Query("""
        select i from PlanInvoiceEntity i
         where i.piStat = :status
           and i.piAmount = :amount
           and i.piStart >= :fromTime
        order by i.piId desc
    """)
    Optional<PlanInvoiceEntity> findRecentPendingSameAmountSimple(@Param("status") PlanEnums.PiStatus status,
                                                                  @Param("amount") BigDecimal amount,
                                                                  @Param("fromTime") LocalDateTime fromTime);

    default Optional<PlanInvoiceEntity> findRecentPendingSameAmount(BigDecimal amount, Duration within) {
        LocalDateTime from = LocalDateTime.now().minus(within == null ? Duration.ofMinutes(10) : within);
        return findRecentPendingSameAmountSimple(PiStatus.PENDING, amount, from);
    }
}
