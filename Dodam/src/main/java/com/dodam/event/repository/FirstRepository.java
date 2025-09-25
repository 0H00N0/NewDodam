package com.dodam.event.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dodam.event.entity.EventNumber;
import com.dodam.event.entity.First;
import com.dodam.member.entity.MemberEntity;

import java.util.List;

public interface FirstRepository extends JpaRepository<First, Long> {
    // 특정 이벤트에 참여한 회원 리스트
    List<First> findByEvent_EvNum(Long evNum);

    // 특정 회원의 선착순 참여 내역
    List<First> findByMember_Mnum(Long mNum);

    // 당첨자 조회
    List<First> findByWinState(Integer winState);
    
    boolean existsByEventAndMember(EventNumber event, MemberEntity member);
    long countByEvent(EventNumber event);

}
