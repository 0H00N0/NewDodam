package com.dodam.event.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dodam.event.entity.LotteryTicket;

import java.util.List;

public interface LotteryTicketRepository extends JpaRepository<LotteryTicket, Long> {
    // 특정 회원이 보유한 추첨권 조회
    List<LotteryTicket> findByMember_Mnum(Long mNum);

    // 상태별 추첨권 조회 (0=미사용, 1=사용, 2=만료)
    List<LotteryTicket> findByStatus(Integer status);
}
