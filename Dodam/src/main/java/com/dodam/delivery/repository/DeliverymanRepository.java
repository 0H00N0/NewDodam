package com.dodam.delivery.repository;

import com.dodam.delivery.entity.DeliverymanEntity;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DeliverymanRepository extends
        JpaRepository<DeliverymanEntity, Long>,
        JpaSpecificationExecutor<DeliverymanEntity> {
	
	// 세션 sid는 mid이므로, MemberEntity.mid로 조회
    Optional<DeliverymanEntity> findByMember_Mid(String mid);
}
