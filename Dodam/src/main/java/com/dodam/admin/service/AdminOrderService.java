// src/main/java/com/dodam/admin/service/AdminOrderService.java
package com.dodam.admin.service;

import com.dodam.admin.dto.AdminOrderListResponseDTO;
import com.dodam.rent.entity.RentEntity;
import com.dodam.rent.repository.RentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminOrderService {

    private final RentRepository rentRepository;

    // 주문 리스트 조회
    public List<AdminOrderListResponseDTO> getAllOrders() {
        // EntityGraph or fetch join 중 하나 선택
        List<RentEntity> rents = rentRepository.findAllByOrderByRenNumDesc();
        // List<RentEntity> rents = rentRepository.findAllWithJoinsOrderByRenNumDesc();

        return rents.stream()
                .map(AdminOrderListResponseDTO::new)
                .toList();
    }

    // 주문 단건 상세 조회
    public AdminOrderListResponseDTO findOrderById(Long orderId) {
        RentEntity rent = rentRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("해당 ID의 주문을 찾을 수 없습니다: " + orderId));
        // 상세도 product/member 접근이 필요하면 여기서 접근(초기화) 보장:
        // rent.getProduct().getProname();
        // rent.getMember().getMname();
        return new AdminOrderListResponseDTO(rent);
    }
}
