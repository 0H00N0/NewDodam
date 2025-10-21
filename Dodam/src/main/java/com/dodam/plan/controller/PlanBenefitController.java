package com.dodam.plan.controller;

import com.dodam.plan.Entity.PlanBenefitEntity;
import com.dodam.plan.repository.PlanBenefitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/planbenefit") // 프론트 호출 경로와 동일
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class PlanBenefitController {

    private final PlanBenefitRepository repo;

    @GetMapping
    public ResponseEntity<?> getByPlanCode(@RequestParam("planCode") String planCode) {
        if (!StringUtils.hasText(planCode)) {
            return ResponseEntity.badRequest().body(Map.of("error", "planCode parameter is required"));
        }

        var hit = repo.findTop1ByPlan_PlanCodeIgnoreCaseOrderByPbIdDesc(planCode);
        if (hit.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "planCode", planCode.toUpperCase(),
                "pbnote", "",
                "note", ""
            ));
        }

        var pb = hit.get();
        String note = pb.getPbNote() == null ? "" : pb.getPbNote();

        return ResponseEntity.ok(Map.of(
            "planCode", planCode.toUpperCase(),
            "pbnote", note,
            "note", note
        ));
    }
}
