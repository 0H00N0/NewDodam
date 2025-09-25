package com.dodam.admin.controller;

import com.dodam.admin.dto.AdminOrderListResponseDTO;
import com.dodam.admin.dto.OrderApprovalRequestDTO;
import com.dodam.admin.dto.OrderRiderRequestDTO;
import com.dodam.admin.service.AdminOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    /**
     * GET /admin/orders
     * 전체 주문 목록 조회 API
     */
    @GetMapping
    public ResponseEntity<List<AdminOrderListResponseDTO>> getAllOrders() {
        List<AdminOrderListResponseDTO> orders = adminOrderService.findAllOrders();
        return ResponseEntity.ok(orders);
    }

    /**
     * PATCH /admin/orders/{orderId}/approval
     * 주문 승인 상태 변경 API
     * @param orderId 변경할 주문 ID
     * @param requestDTO 변경할 상태 값
     */
    @PatchMapping("/{orderId}/approval")
    public ResponseEntity<AdminOrderListResponseDTO> updateApproval(
            @PathVariable("orderId") Long orderId,
            @RequestBody OrderApprovalRequestDTO requestDTO) {
        AdminOrderListResponseDTO updatedOrder = adminOrderService.updateApprovalStatus(orderId, requestDTO);
        return ResponseEntity.ok(updatedOrder);
    }

    /**
     * PATCH /admin/orders/{orderId}/rider
     * 배송 기사 배정 API
     * @param orderId 배정할 주문 ID
     * @param requestDTO 배정할 기사 및 운송장 정보
     */
    @PatchMapping("/{orderId}/rider")
    public ResponseEntity<AdminOrderListResponseDTO> assignRider(
            @PathVariable("orderId") Long orderId,
            @RequestBody OrderRiderRequestDTO requestDTO) {
        AdminOrderListResponseDTO updatedOrder = adminOrderService.assignRider(orderId, requestDTO);
        return ResponseEntity.ok(updatedOrder);
    }
 // AdminOrderController.java

    /**
     * GET /admin/orders/{orderId}
     * 특정 주문 상세 조회 API
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<AdminOrderListResponseDTO> getOrderById(@PathVariable("orderId") Long orderId) {
        AdminOrderListResponseDTO order = adminOrderService.findOrderById(orderId);
        return ResponseEntity.ok(order);
    }
}