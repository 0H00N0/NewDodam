package com.dodam.admin.controller;

import com.dodam.admin.dto.AdminOrderListResponseDTO;
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

    // 전체 주문 목록 조회
    @GetMapping
    public ResponseEntity<List<AdminOrderListResponseDTO>> getOrders() {
        return ResponseEntity.ok(adminOrderService.getAllOrders());
    }

    // ✅ 수정된 코드: 특정 주문 상세 조회
    @GetMapping("/{orderId}")
    public ResponseEntity<AdminOrderListResponseDTO> getOrderById(@PathVariable("orderId") Long orderId) {
        return ResponseEntity.ok(adminOrderService.findOrderById(orderId));
    }
}