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

    /** ✅ A. JSON 패치 (권장 경로) */
    @PatchMapping(
        value = "/{renNum}/tracking",
        consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE },
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AdminOrderListResponseDTO> updateTrackingJsonOrForm(
            @PathVariable Long renNum,
            @RequestBody(required = false) TrackingUpdateRequest body) {

        String tn = body != null ? body.getTrackingNumber() : null;
        String normalized = (tn == null || tn.trim().isEmpty()) ? null : tn.trim();

        adminOrderService.updateTrackingNumber(renNum, normalized);
        return ResponseEntity.ok(adminOrderService.findOrderById(renNum));
    }

    /** ✅ B. 쿼리파라미터 fallback: /{renNum}/tracking?value=XXX */
    @PatchMapping(value = "/{renNum}/tracking", params = "value", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AdminOrderListResponseDTO> updateTrackingParam(
            @PathVariable Long renNum, @RequestParam("value") String value) {
        String normalized = (value == null || value.trim().isEmpty()) ? null : value.trim();
        adminOrderService.updateTrackingNumber(renNum, normalized);
        return ResponseEntity.ok(adminOrderService.findOrderById(renNum));
    }
}
