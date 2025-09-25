package com.dodam.admin.service;

import com.dodam.admin.dto.AdminOrderListResponseDTO;
import com.dodam.admin.dto.OrderApprovalRequestDTO;
import com.dodam.admin.dto.OrderRiderRequestDTO;
import com.dodam.rent.entity.RentEntity;
import com.dodam.rent.repository.RentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminOrderService {

    private final RentRepository rentRepository;

    /**
     * 모든 주문(대여) 목록을 조회합니다.
     * @return 관리자용 주문 목록 DTO 리스트
     */
    @Transactional(readOnly = true)
    public List<AdminOrderListResponseDTO> findAllOrders() {
        return rentRepository.findAll().stream()
                .map(AdminOrderListResponseDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * 주문의 승인 상태를 변경합니다.
     * @param orderId 상태를 변경할 주문의 ID
     * @param requestDTO 새로운 승인 상태 정보
     * @return 변경된 주문 정보 DTO
     */
    @Transactional
    public AdminOrderListResponseDTO updateApprovalStatus(Long orderId, OrderApprovalRequestDTO requestDTO) {
        RentEntity rent = rentRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("해당 ID의 주문을 찾을 수 없습니다: " + orderId));

        rent.setRenApproval(requestDTO.getRenApproval());
        
        return new AdminOrderListResponseDTO(rent);
    }

    /**
     * 주문에 배송 기사와 운송장 번호를 배정합니다.
     * @param orderId 배정할 주문의 ID
     * @param requestDTO 배송 기사 및 운송장 번호 정보
     * @return 변경된 주문 정보 DTO
     */
    @Transactional
    public AdminOrderListResponseDTO assignRider(Long orderId, OrderRiderRequestDTO requestDTO) {
        RentEntity rent = rentRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("해당 ID의 주문을 찾을 수 없습니다: " + orderId));

        rent.setRenRider(requestDTO.getRenRider());
        rent.setTrackingNumber(requestDTO.getTrackingNumber());
        // 필요 시 배송 상태 변경 로직 추가 (예: rent.setRenShip("배송중");)

        return new AdminOrderListResponseDTO(rent);
    }
    /**
     * ID로 특정 주문 정보를 조회합니다.
     * @param orderId 조회할 주문의 ID
     * @return 관리자용 주문 상세 DTO
     */
    @Transactional(readOnly = true)
    public AdminOrderListResponseDTO findOrderById(Long orderId) {
        RentEntity rent = rentRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("해당 ID의 주문을 찾을 수 없습니다: " + orderId));
        return new AdminOrderListResponseDTO(rent);
    }
}