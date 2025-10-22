package com.dodam.event.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.dodam.event.entity.EventNumber;
import com.dodam.event.entity.First;
import com.dodam.member.entity.MemberEntity;

import java.util.List;

public interface FirstRepository extends JpaRepository<First, Long> {

    // 특정 이벤트에 참여한 회원 리스트
    List<First> findByEvent_EvNum(Long evNum);

    // 특정 이벤트 참여자 리스트 (참여 일시 오름차순)
    List<First> findByEvent_EvNumOrderByFdateAsc(Long evNum);

    // 특정 회원의 선착순 참여 내역
    List<First> findByMember_Mnum(Long mNum);

    // 당첨자 조회 (winState = 1 같은 식으로 구분)
    List<First> findByWinState(Integer winState);

    // 특정 이벤트 + 특정 회원 중복 응모 방지 체크
    boolean existsByEventAndMember(EventNumber event, MemberEntity member);

    // 특정 이벤트에 참여한 총 인원 수
    long countByEvent(EventNumber event);

    // ✅ 이벤트 삭제 시 연관된 선착순 참여 데이터 삭제 (ORA-02292 FK 제약조건 해결)
    void deleteByEvent(EventNumber event);

    // ✅ 또는 evNum으로 삭제하는 방법 (선택적으로 사용)
    @Modifying
    @Query("DELETE FROM First f WHERE f.event.evNum = :evNum")
    void deleteByEventEvNum(@Param("evNum") Long evNum);

    // ✅ 또는 이벤트 객체의 evNum으로 삭제 (더 명확한 방법)
    void deleteByEvent_EvNum(Long evNum);
}