package com.dodam.buy.repository;

import com.dodam.buy.entity.BuyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BuyRepository extends JpaRepository<BuyEntity, Long> {
    // 필요시 커스텀 쿼리 메서드 추가
}