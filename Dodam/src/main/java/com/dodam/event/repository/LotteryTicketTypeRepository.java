package com.dodam.event.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dodam.event.entity.LotteryTicketType;

public interface LotteryTicketTypeRepository extends JpaRepository<LotteryTicketType, Long> {
    // 추첨권 이름으로 검색
    LotteryTicketType findByLotTypeName(String lotTypeName);
}
