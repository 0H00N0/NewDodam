package com.dodam.admin.controller;

import com.dodam.admin.dto.AdminOrderListResponseDTO;
import com.dodam.admin.dto.TrackingUpdateRequest;
import com.dodam.admin.service.AdminOrderService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

    // 특정 주문 상세 조회
    @GetMapping("/{orderId}")
    public ResponseEntity<AdminOrderListResponseDTO> getOrderById(@PathVariable("orderId") Long orderId) {
        return ResponseEntity.ok(adminOrderService.findOrderById(orderId));
    }

 // AdminOrderController.java
    @PatchMapping(value = "/{renNum}/tracking", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AdminOrderListResponseDTO> updateTrackingUnified(
            @PathVariable("renNum") Long renNum,  // <-- 이름 명시!
            @RequestParam(value = "value", required = false) String qp,
            @RequestBody(required = false) Map<String, Object> body) {
        String tn = qp;
        if (tn == null && body != null) {
            Object v = body.getOrDefault("trackingNumber",
                      body.getOrDefault("trackingNum", body.get("invoiceNo")));
            if (v != null) tn = v.toString();
        }
        String normalized = (tn == null || tn.trim().isEmpty()) ? null : tn.trim();

        adminOrderService.updateTrackingNumber(renNum, normalized);
        return ResponseEntity.ok(adminOrderService.findOrderById(renNum));
    }


}
