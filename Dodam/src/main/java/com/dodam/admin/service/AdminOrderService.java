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

    public List<AdminOrderListResponseDTO> getAllOrders() {
        List<RentEntity> rents = rentRepository.findAllWithLeftJoinsOrderByRenNumDesc();
        return rents.stream().map(AdminOrderListResponseDTO::new).toList();
    }

    public AdminOrderListResponseDTO findOrderById(Long orderId) {
        RentEntity rent = rentRepository.findByRenNumWithJoins(orderId)
                .orElseThrow(() -> new EntityNotFoundException("해당 ID의 주문을 찾을 수 없습니다: " + orderId));
        return new AdminOrderListResponseDTO(rent);
    }
}
