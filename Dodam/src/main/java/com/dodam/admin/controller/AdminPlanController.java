package com.dodam.admin.controller;

import com.dodam.admin.dto.AdminPlanDto;
import com.dodam.admin.dto.ApiResponseDTO;
import com.dodam.admin.service.AdminPlanService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/plans")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class AdminPlanController {

    private final AdminPlanService adminPlanService;

    @PostMapping
    public ResponseEntity<AdminPlanDto.Response> createPlan(@RequestBody AdminPlanDto.CreateRequest requestDto) {
        AdminPlanDto.Response responseDto = adminPlanService.createPlan(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<AdminPlanDto.Response>> getAllPlans() {
        List<AdminPlanDto.Response> plans = adminPlanService.getAllPlans();
        return ResponseEntity.ok(plans);
    }

    @GetMapping("/{planId}")
    public ResponseEntity<AdminPlanDto.Response> getPlanById(@PathVariable("planId") Long planId) {
        AdminPlanDto.Response plan = adminPlanService.getPlan(planId);
        return ResponseEntity.ok(plan);
    }

    @PutMapping("/{planId}")
    public ResponseEntity<AdminPlanDto.Response> updatePlan(@PathVariable("planId") Long planId, @RequestBody AdminPlanDto.UpdateRequest requestDto) {
        AdminPlanDto.Response updatedPlan = adminPlanService.updatePlan(planId, requestDto);
        return ResponseEntity.ok(updatedPlan);
    }

    @DeleteMapping("/{planId}")
    public ResponseEntity<ApiResponseDTO> deletePlan(@PathVariable("planId") Long planId) {
        adminPlanService.deletePlan(planId);
        ApiResponseDTO response = new ApiResponseDTO(true, "Plan deleted successfully.");
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponseDTO> handleEntityNotFoundException(EntityNotFoundException ex) {
        ApiResponseDTO response = new ApiResponseDTO(false, ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDTO> handleGlobalException(Exception ex) {
        ApiResponseDTO response = new ApiResponseDTO(false, "An error occurred during server processing: " + ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
