// src/main/java/com/dodam/admin/controller/AdminOrderController.java
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

    // 리스트
    @GetMapping
    public ResponseEntity<List<AdminOrderListResponseDTO>> getOrders() {
        return ResponseEntity.ok(adminOrderService.getAllOrders());
    }

    // 상세
    @GetMapping("/{orderId}")
    public ResponseEntity<AdminOrderListResponseDTO> getOrderById(@PathVariable Long orderId) {
        return ResponseEntity.ok(adminOrderService.findOrderById(orderId));
    }
}
