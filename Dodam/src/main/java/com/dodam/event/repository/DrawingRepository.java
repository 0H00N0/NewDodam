package com.dodam.event.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dodam.event.entity.Drawing;
import com.dodam.event.entity.EventNumber;
import com.dodam.member.entity.MemberEntity;

import java.util.List;

public interface DrawingRepository extends JpaRepository<Drawing, Long> {
    // 특정 이벤트의 추첨 결과 조회
    List<Drawing> findByEvent_EvNum(Long evNum);

    // 수정 후
    List<Drawing> findByMember_Mnum(Long mnum);

    // 당첨 상태별 조회 (0=미당첨, 1=당첨, 2=무효)
    List<Drawing> findByDrawState(Integer drawState);
    
    boolean existsByEventAndMember(EventNumber event, MemberEntity member);

}
