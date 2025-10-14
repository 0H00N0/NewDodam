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

    @GetMapping
    public ResponseEntity<List<AdminOrderListResponseDTO>> getOrders() {
        return ResponseEntity.ok(adminOrderService.getAllOrders());
    }

    // 숫자만 상세로 들어오도록 백엔드 경로 정규식
    @GetMapping("/orderId")
    public ResponseEntity<AdminOrderListResponseDTO> getOrderById(@PathVariable("orderId") Long orderId) {
        return ResponseEntity.ok(adminOrderService.findOrderById(orderId));
    }
}
