package com.dodam.plan.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.dodam.plan.Entity.PlanMember;
import com.dodam.plan.enums.PlanEnums.PmStatus;

public interface PlanMemberRepository extends JpaRepository<PlanMember, Long> {

    /* ====== 프로퍼티명 기반 쿼리 (pmStatus로 변경) ====== */

    List<PlanMember> findByMember_MnumAndPmStatus(Long mnum, PmStatus pmStatus);

    List<PlanMember> findByPmNextBilBeforeAndPmStatus(LocalDateTime now, PmStatus pmStatus);

    Optional<PlanMember> findFirstByMember_MnumAndPmStatus(Long mnum, PmStatus pmStatus);

    Optional<PlanMember> findTopByMember_MidOrderByPmIdDesc(String mid);

    /** MemberEntity.mid 기준으로 PlanMember 찾기 */
    Optional<PlanMember> findByMember_Mid(String mid);

    /** MemberEntity.mnum(PK) 기준으로 PlanMember 찾기 */
    Optional<PlanMember> findByMember_Mnum(Long mnum);

    /* ====== 활성 구독 1건 조회 (JPQL) ====== */
    @Query("""
        select m
          from PlanMember m
         where m.member.mid = :mid
           and m.pmStatus = com.dodam.plan.enums.PlanEnums$PmStatus.ACTIVE
           and (m.pmTermStart is null or m.pmTermStart <= :now)
           and (m.pmTermEnd   is null or m.pmTermEnd   >= :now)
        """)
    Optional<PlanMember> findActiveByMid(@Param("mid") String mid, @Param("now") LocalDateTime now);

    /* ====== 상태 갱신 (결제 확정 후 활성화) ====== */

    /**
     * 기존 호출 시그니처 유지용(native):
     * now 파라미터는 사용하지 않음. Oracle 컬럼명은 PMSTATUS 로 반영.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = "UPDATE PLANMEMBER SET PMSTATUS = 'ACTIVE' WHERE PMID = :pmId", nativeQuery = true)
    int activateAfterPaid(@Param("pmId") Long pmId, @Param("now") LocalDateTime now);

    /**
     * JPQL 업데이트 버전 (상태를 파라미터로 설정)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
        update PlanMember m
           set m.pmStatus = :stat
         where m.pmId = :pmId
    """)
    int activateByPmstatus(@Param("pmId") Long pmId,
                           @Param("stat") PmStatus stat);

    /** MemberEntity.mnum(PK) 기준으로 모든 PlanMember 조회 (전체 이력) */
    List<PlanMember> findAllByMember_Mnum(Long mnum);
}
