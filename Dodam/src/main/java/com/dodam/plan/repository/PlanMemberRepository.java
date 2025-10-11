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
import com.dodam.plan.enums.PlanEnums;
import com.dodam.plan.enums.PlanEnums.PmStatus;

public interface PlanMemberRepository extends JpaRepository<PlanMember, Long> {
	List<PlanMember> findByMember_MnumAndPmStat(Long mnum, PmStatus pmStat);

	List<PlanMember> findByPmNextBilBeforeAndPmStat(LocalDateTime now, PmStatus pmStat);

	Optional<PlanMember> findFirstByMember_MnumAndPmStat(Long mnum, PmStatus pmStat);

	Optional<PlanMember> findTopByMember_MidOrderByPmIdDesc(String mid);

	/**
	 * MemberEntity.mid 기준으로 PlanMember 찾기
	 */
	Optional<PlanMember> findByMember_Mid(String mid);

	/**
	 * MemberEntity.mnum(PK) 기준으로 PlanMember 찾기
	 */
	Optional<PlanMember> findByMember_Mnum(Long mnum);
	
	/*결제 취소 관련*/
	@Query("""
	        select m
	          from PlanMember m
	         where m.member.mid = :mid
	           and m.pmStat = com.dodam.plan.enums.PlanEnums$PmStatus.ACTIVE
	           and (m.pmTermStart is null or m.pmTermStart <= :now)
	           and (m.pmTermEnd   is null or m.pmTermEnd   >= :now)
	    """)
	Optional<PlanMember> findActiveByMid(@Param("mid") String mid, @Param("now") LocalDateTime now);
	
	/**
     * 기존 호출 시그니처를 유지: (pmId, now)
     * now 파라미터는 쿼리에서 사용하지 않지만, nativeQuery 이므로 검증 에러가 나지 않습니다.
     * pmstat이 ENUM이든 VARCHAR든, DB에는 'ACTIVE' 값이 들어갑니다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = "UPDATE PLANMEMBER SET PMSTAT = 'ACTIVE' WHERE PMID = :pmId", nativeQuery = true)
    int activateAfterPaid(@Param("pmId") Long pmId, @Param("now") LocalDateTime now);
	
	/**
     * 결제 완료 후 회원 구독 상태를 활성화로 전환.
     * - PlanMember 엔티티에 pmstat(ENUM/문자열) 컬럼만 있다고 가정.
     * - updatedAt 같은 필드는 건드리지 않음.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
        update PlanMember m
           set m.pmStat = :stat
         where m.pmId = :pmId
    """)
    int activateByPmstat(@Param("pmId") Long pmId,
                         @Param("stat") PmStatus stat);
    /**
     * MemberEntity.mnum(PK) 기준으로 모든 PlanMember 조회 (전체 이력)
     */
    List<PlanMember> findAllByMember_Mnum(Long mnum);
    

}
