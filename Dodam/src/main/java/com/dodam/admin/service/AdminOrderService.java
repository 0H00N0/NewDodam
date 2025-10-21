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

    @Transactional
    public void updateTrackingNumber(Long renNum, String trackingNumber) {
        RentEntity rent = rentRepository.findById(renNum)
            .orElseThrow(() -> new EntityNotFoundException("주문을 찾을 수 없습니다: " + renNum));

        rent.setTrackingNumber(trackingNumber); // null이면 DB에서 비움
        // 상태 자동 전이를 원하면:
        // if (trackingNumber != null && !trackingNumber.isBlank()) {
        //     rent.setRenShip(RentEntity.ShipStatus.SHIPPING);
        // }
    }

    // ✨ 배송 상태 변경
    @Transactional
    public void updateShipStatus(Long renNum, String status) {
        RentEntity rent = rentRepository.findById(renNum)
                .orElseThrow(() -> new EntityNotFoundException("주문을 찾을 수 없습니다: " + renNum));

        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("상태 값이 비어 있습니다. 허용: SHIPPING, DELIVERED");
        }
        String up = status.trim().toUpperCase();

        try {
            RentEntity.ShipStatus parsed = RentEntity.ShipStatus.valueOf(up);
            rent.setRenShip(parsed);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("허용되지 않는 상태: " + status + " (가능: SHIPPING, DELIVERED)");
        }
    }
}
