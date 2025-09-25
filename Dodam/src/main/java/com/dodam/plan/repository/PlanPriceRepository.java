package com.dodam.plan.repository;

import com.dodam.plan.Entity.PlanPriceEntity;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface PlanPriceRepository extends JpaRepository<PlanPriceEntity, Long> {

    /* ───────── 기본 목록 조회 ───────── */
    List<PlanPriceEntity> findByPlan_PlanIdAndPpriceActiveTrue(Long planId);

    @EntityGraph(attributePaths = {"pterm"})
    List<PlanPriceEntity> findByPlan_PlanIdAndPpriceActiveTrueOrderByPterm_PtermMonth(Long planId);

    @Query("""
        select p
          from PlanPriceEntity p
          join fetch p.pterm t
         where p.plan.planId = :planId
           and p.ppriceActive = :active
         order by t.ptermMonth
    """)
    List<PlanPriceEntity> findWithTermsByPlanAndActive(@Param("planId") Long planId,
                                                       @Param("active") Boolean active);

    /* ───────── 단건 조회: plan + termId + mode 정확히 (ACTIVE=true) ───────── */
    // (기존 서비스 시그니처를 그대로 살림)
    Optional<PlanPriceEntity>
    findFirstByPlan_PlanIdAndPterm_PtermIdAndPpriceBilModeAndPpriceActiveTrue(
            Long planId, Long ptermId, String mode);

    // 대소문자 무시 버전 (더 관대)
    Optional<PlanPriceEntity>
    findFirstByPlan_PlanIdAndPterm_PtermIdAndPpriceBilModeIgnoreCaseAndPpriceActiveTrue(
            Long planId, Long ptermId, String mode);

    // months(=ptermMonth)로 직접 찾는 오버로드
    Optional<PlanPriceEntity>
    findFirstByPlan_PlanIdAndPterm_PtermMonthAndPpriceBilModeIgnoreCaseAndPpriceActiveTrue(
            Long planId, int months, String mode);

    // enum을 그대로 받는 브릿지(default): 서비스가 enum을 넘겨도 안전
    default Optional<PlanPriceEntity> findFirstByPlanAndPtermAndMode(Long planId, Long ptermId, Enum<?> mode) {
        return (mode == null)
                ? Optional.empty()
                : findFirstByPlan_PlanIdAndPterm_PtermIdAndPpriceBilModeIgnoreCaseAndPpriceActiveTrue(
                        planId, ptermId, mode.name());
    }

    /* ───────── 정확 모드 없을 때 AUTO fallback ───────── */
    @Query("""
        select pp
          from PlanPriceEntity pp
         where pp.plan.planId = :planId
           and pp.pterm.ptermId = :ptermId
           and pp.ppriceActive = true
           and (pp.ppriceBilMode = :mode or pp.ppriceBilMode = 'AUTO')
         order by case when pp.ppriceBilMode = :mode then 0 else 1 end
    """)
    Optional<PlanPriceEntity> findBestPrice(@Param("planId") Long planId,
                                            @Param("ptermId") Long ptermId,
                                            @Param("mode") String mode);

    /* ───────── 이름 유지 요청: findByPlanIdAndMonths (Native) ───────── */
    @Query(value = """
        SELECT p.*
          FROM PLANPRICE p
          JOIN PLANTERMS t ON t.PTERM_ID = p.PTERM_ID
         WHERE p.PLAN_ID = :planId
           AND t.PTERM_MONTH = :months
           AND p.PPRICE_ACTIVE = 1
         FETCH FIRST 1 ROWS ONLY
    """, nativeQuery = true)
    Optional<PlanPriceEntity> findByPlanIdAndMonths(@Param("planId") Long planId,
                                                    @Param("months") int months);
    
    @Query("select p.pterm.ptermId from PlanPriceEntity p where p.ppriceId = :priceId")
    Long findTermIdByPriceId(@Param("priceId") Long priceId);
}
