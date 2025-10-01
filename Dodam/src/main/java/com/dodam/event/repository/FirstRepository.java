package com.dodam.event.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.dodam.event.entity.EventNumber;
import com.dodam.event.entity.First;
import com.dodam.member.entity.MemberEntity;

import java.util.List;

public interface FirstRepository extends JpaRepository<First, Long> {
    
    // 특정 이벤트에 참여한 회원 리스트
    List<First> findByEvent_EvNum(Long evNum);

    // ✅ [최종 수정] OrderByFdateAsc -> OrderByFDateAsc (소문자 d를 다시 대문자 D로)
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
}