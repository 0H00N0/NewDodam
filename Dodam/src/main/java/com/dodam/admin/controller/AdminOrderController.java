package com.dodam.admin.controller;

import com.dodam.admin.dto.AdminOrderListResponseDTO;
import com.dodam.admin.service.AdminOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// 디버깅용 Security import
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    /** 현재 SecurityContext 상태를 콘솔/로그로 출력 */
    private void logAuth(String where) {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null) {
            log.info("[AUTH@{}] null", where);
            return;
        }
        String authorities = a.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
        log.info("[AUTH@{}] principal={}, authorities=[{}], details={}",
                where, a.getName(), authorities, a.getDetails());
    }

    /** 입력(Map/Body/Query)에서 상태 문자열 뽑아내기 (디버깅 편의용 유틸) */
    private String resolveStatusFromBodyOrQuery(String qp, Map<String, Object> body) {
        String next = qp;
        if (next == null && body != null) {
            Object v = body.getOrDefault("status",
                    body.getOrDefault("renShip",
                    body.getOrDefault("shippingStatus", null)));
            if (v != null) next = v.toString();
        }
        return (next == null || next.isBlank()) ? null : next.trim();
    }

    /** 입력(Map/Body/Query)에서 운송장 문자열 뽑아내기 (디버깅 편의용 유틸) */
    private String resolveTrackingFromBodyOrQuery(String qp, Map<String, Object> body) {
        String tn = qp;
        if (tn == null && body != null) {
            Object v = body.getOrDefault("trackingNumber",
                    body.getOrDefault("trackingNum", body.get("invoiceNo")));
            if (v != null) tn = v.toString();
        }
        return (tn == null || tn.trim().isEmpty()) ? null : tn.trim();
    }

    // =========================
    //      디버깅 보조 API
    // =========================

    /** 현재 인증/권한 상태를 바로 확인 (GET /admin/orders/_auth) */
    @GetMapping("/_auth")
    public ResponseEntity<Map<String, Object>> debugAuth() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null) {
            return ResponseEntity.ok(Map.of("auth", "null"));
        }
        List<String> roles = a.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).toList();
        return ResponseEntity.ok(Map.of(
                "principal", a.getName(),
                "authorities", roles,
                "details", String.valueOf(a.getDetails())
        ));
    }

    // =========================
    //        실제 기능
    // =========================

    // 전체 주문 목록 조회
    @GetMapping
    public ResponseEntity<List<AdminOrderListResponseDTO>> getOrders() {
        logAuth("GET /admin/orders");
        return ResponseEntity.ok(adminOrderService.getAllOrders());
    }

    // 특정 주문 상세 조회
    @GetMapping("/{orderId}")
    public ResponseEntity<AdminOrderListResponseDTO> getOrderById(@PathVariable("orderId") Long orderId) {
        logAuth("GET /admin/orders/" + orderId);
        return ResponseEntity.ok(adminOrderService.findOrderById(orderId));
    }

    // 운송장 저장 (통합 입력: 쿼리파라미터 or 바디)
    @PatchMapping(value = "/{renNum}/tracking", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AdminOrderListResponseDTO> updateTrackingUnified(
            @PathVariable("renNum") Long renNum,
            @RequestParam(value = "value", required = false) String qp,
            @RequestBody(required = false) Map<String, Object> body) {

        logAuth("PATCH /admin/orders/" + renNum + "/tracking");
        String normalized = resolveTrackingFromBodyOrQuery(qp, body);
        log.info("[REQ tracking] renNum={}, qp='{}', bodyKeys={}, resolved='{}'",
                renNum, qp, body == null ? "null" : body.keySet(), normalized);

        adminOrderService.updateTrackingNumber(renNum, normalized);
        return ResponseEntity.ok(adminOrderService.findOrderById(renNum));
    }

    // ✨ 배송 상태 변경 (쿼리파라미터 value 또는 바디 status/renShip/shippingStatus 지원)
    @PatchMapping(value = "/{renNum}/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AdminOrderListResponseDTO> updateStatus(
            @PathVariable("renNum") Long renNum,
            @RequestParam(value = "value", required = false) String qp,
            @RequestBody(required = false) Map<String, Object> body) {

        logAuth("PATCH /admin/orders/" + renNum + "/status");
        String next = resolveStatusFromBodyOrQuery(qp, body);
        log.info("[REQ status] renNum={}, qp='{}', bodyKeys={}, resolved='{}'",
                renNum, qp, body == null ? "null" : body.keySet(), next);

        adminOrderService.updateShipStatus(renNum, next);
        return ResponseEntity.ok(adminOrderService.findOrderById(renNum));
    }

    @ExceptionHandler({ IllegalArgumentException.class, EntityNotFoundException.class })
    public ResponseEntity<Map<String, String>> handleBadRequest(RuntimeException e) {
        log.warn("[EX] {}", e.toString());
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
}
