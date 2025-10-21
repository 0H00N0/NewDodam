package com.dodam.admin.controller;

import com.dodam.admin.dto.AdminPlanDto;
import com.dodam.admin.dto.ApiResponseDTO;
import com.dodam.admin.dto.RefundRequestDTO;
import com.dodam.plan.Entity.PlanAttemptEntity;
import com.dodam.plan.Entity.PlanInvoiceEntity;
import com.dodam.plan.Entity.PlanMember;
import com.dodam.plan.Entity.PlanRefundEntity;
import com.dodam.admin.service.AdminPlanService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/plans")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class AdminPlanController {

    private final AdminPlanService adminPlanService;

    // -------------------------------
    // ✅ Plan CRUD
    // -------------------------------

    @PostMapping
    public ResponseEntity<AdminPlanDto.Response> createPlan(@RequestBody AdminPlanDto.CreateRequest requestDto) {
        AdminPlanDto.Response responseDto = adminPlanService.createPlan(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<AdminPlanDto.Response>> getAllPlans() {
        return ResponseEntity.ok(adminPlanService.getAllPlans());
    }

    @GetMapping("/{planId}")
    public ResponseEntity<AdminPlanDto.Response> getPlanById(@PathVariable("planId") Long planId) {
        return ResponseEntity.ok(adminPlanService.getPlan(planId));
    }

    @PutMapping("/{planId}")
    public ResponseEntity<AdminPlanDto.Response> updatePlan(
            @PathVariable Long planId,
            @RequestBody AdminPlanDto.UpdateRequest requestDto) {
        return ResponseEntity.ok(adminPlanService.updatePlan(planId, requestDto));
    }

    @DeleteMapping("/{planId}")
    public ResponseEntity<ApiResponseDTO> deletePlan(@PathVariable("planId") Long planId) {
        adminPlanService.deletePlan(planId);
        return ResponseEntity.ok(new ApiResponseDTO(true, "Plan deleted successfully."));
    }

    // -------------------------------
    // ✅ 구독 관리
    // -------------------------------

    @GetMapping("/subscriptions/member/{memberId}")
    public ResponseEntity<List<PlanMember>> getMemberSubscriptions(@PathVariable Long memberId) {
        return ResponseEntity.ok(adminPlanService.getMemberSubscriptions(memberId));
    }

    @PatchMapping("/subscriptions/{pmId}/status")
    public ResponseEntity<PlanMember> updateSubscriptionStatus(
            @PathVariable Long pmId,
            @RequestBody Map<String, String> request) {
        String status = request.get("status");
        return ResponseEntity.ok(adminPlanService.updateSubscriptionStatus(pmId, status));
    }
    @GetMapping("/subscriptions")
    public ResponseEntity<List<AdminPlanDto.PlanMemberDto>> getAllSubscriptions() {
        return ResponseEntity.ok(
            adminPlanService.getAllSubscriptions().stream()
                .map(AdminPlanDto.PlanMemberDto::fromEntity)
                .toList()
        );
    }

    @GetMapping("/invoices")
    public ResponseEntity<List<AdminPlanDto.PlanInvoiceDto>> getAllInvoices() {
        return ResponseEntity.ok(
            adminPlanService.getAllInvoices().stream()
                .map(AdminPlanDto.PlanInvoiceDto::fromEntity)
                .toList()
        );
    }


    // -------------------------------
    // ✅ 결제 내역
    // -------------------------------

    @GetMapping("/invoices/member/{memberId}")
    public ResponseEntity<List<PlanInvoiceEntity>> getMemberInvoices(@PathVariable Long memberId) {
        return ResponseEntity.ok(adminPlanService.getMemberInvoices(memberId));
    }

    @GetMapping("/invoices/{piId}")
    public ResponseEntity<PlanInvoiceEntity> getInvoiceDetail(@PathVariable Long piId) {
        return ResponseEntity.ok(adminPlanService.getInvoiceDetail(piId));
    }

    // -------------------------------
    // ✅ 결제 실패 로그
    // -------------------------------

    @GetMapping("/attempts/invoice/{piId}")
    public ResponseEntity<List<PlanAttemptEntity>> getInvoiceAttempts(@PathVariable Long piId) {
        return ResponseEntity.ok(adminPlanService.getInvoiceAttempts(piId));
    }

    // -------------------------------
    // ✅ 환불 관리
    // -------------------------------

    @PostMapping("/refunds")
    public ResponseEntity<PlanRefundEntity> createRefund(@RequestBody RefundRequestDTO refundRequest) {
        return new ResponseEntity<>(adminPlanService.createRefund(refundRequest), HttpStatus.CREATED);
    }

    @PatchMapping("/refunds/{refundId}/status")
    public ResponseEntity<PlanRefundEntity> updateRefundStatus(
            @PathVariable Long refundId,
            @RequestBody Map<String, String> request) {
        String status = request.get("status");
        return ResponseEntity.ok(adminPlanService.updateRefundStatus(refundId, status));
    }

    @GetMapping("/refunds")
    public ResponseEntity<List<PlanRefundEntity>> getRefunds(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(adminPlanService.getRefunds(status));
    }

    // -------------------------------
    // ✅ 예외 처리
    // -------------------------------

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponseDTO> handleEntityNotFoundException(EntityNotFoundException ex) {
        return new ResponseEntity<>(new ApiResponseDTO(false, ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDTO> handleGlobalException(Exception ex) {
        return new ResponseEntity<>(new ApiResponseDTO(false,
                "Server error: " + ex.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
