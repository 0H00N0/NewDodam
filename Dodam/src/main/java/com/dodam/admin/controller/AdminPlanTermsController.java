package com.dodam.admin.controller;

import com.dodam.plan.Entity.PlanTermsEntity;
import com.dodam.admin.service.AdminPlanTermsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/planterms")
@RequiredArgsConstructor
public class AdminPlanTermsController {

    private final AdminPlanTermsService adminPlanTermsService;

    // 모든 기간 옵션 조회
    @GetMapping
    public List<PlanTermsEntity> getAllTerms() {
        return adminPlanTermsService.getAllTerms();
    }

    // 특정 ID로 조회
    @GetMapping("/{id}")
    public PlanTermsEntity getTermById(@PathVariable Long id) {
        return adminPlanTermsService.getTermById(id);
    }

    // 특정 개월수로 조회
    @GetMapping("/month/{month}")
    public PlanTermsEntity getTermByMonth(@PathVariable Integer month) {
        return adminPlanTermsService.getTermByMonth(month);
    }
}
