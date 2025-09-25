package com.dodam.event.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dodam.event.entity.EventNumber;

import java.util.List;

public interface EventNumberRepository extends JpaRepository<EventNumber, Long> {
    // 이벤트 상태별 조회 (0=예정, 1=진행중, 2=종료)
    List<EventNumber> findByStatus(Integer status);

    // 이름으로 이벤트 검색
    List<EventNumber> findByEvNameContaining(String keyword);
}

