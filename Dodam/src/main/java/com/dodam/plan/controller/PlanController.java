package com.dodam.plan.controller;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dodam.plan.service.PlanService;
import com.dodam.plan.Entity.PlansEntity;
import com.dodam.plan.dto.PlanDTO;
import com.dodam.plan.dto.PlanDetailDTO;
import com.dodam.plan.repository.PlanBenefitRepository;
import com.dodam.plan.repository.PlanPriceRepository;
import com.dodam.plan.repository.PlansRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/plans")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class PlanController {
	private final PlanService ps;
	private final PlansRepository pr;
	private final PlanBenefitRepository pbr;
	private final PlanPriceRepository ppr;
	@GetMapping
	public List<PlanDTO> list(){
		return ps.getActivePlans();
	}
	
	@GetMapping("/{planCode}")
	public ResponseEntity<PlanDetailDTO> detail(@PathVariable("planCode") String planCode) {
	  PlansEntity plan = pr.findByPlanCodeIgnoreCase(planCode)
	      .orElseThrow(() -> new NoSuchElementException("플랜을 찾을 수 없습니다: " + planCode));
	  var benefit = pbr.findFirstByPlan(plan).orElse(null);
	  var prices  = ppr.findByPlan_PlanIdAndPpriceActiveTrue(plan.getPlanId());
	  return ResponseEntity.ok(PlanDetailDTO.of(plan, benefit, prices));
	}
}
