package com.dodam.plan.controller;

import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.dodam.plan.Entity.PlanNameEntity;
import com.dodam.plan.Entity.PlanPriceEntity;
import com.dodam.plan.Entity.PlansEntity;
import com.dodam.plan.dto.PlanDTO;
import com.dodam.plan.dto.PlanDetailDTO;
import com.dodam.plan.repository.PlanBenefitRepository;
import com.dodam.plan.repository.PlanPriceRepository;
import com.dodam.plan.repository.PlansRepository;
import com.dodam.plan.service.PlanPriceService;
import com.dodam.plan.service.PlanService;

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

    // ✅ 금액 산출은 전부 PricingService로 통일
    private final PlanPriceService pricingService;

    @GetMapping
    public List<PlanDTO> list() {
        return ps.getActivePlans();
    }

    @GetMapping("/{planCode}")
    public ResponseEntity<PlanDetailDTO> detail(@PathVariable("planCode") String planCode) {
        PlansEntity plan = pr.findByPlanCodeIgnoreCase(planCode)
                .orElseThrow(() -> new NoSuchElementException("플랜을 찾을 수 없습니다: " + planCode));

        var benefit = pbr.findFirstByPlan(plan).orElse(null);
        var prices  = ppr.findByPlan_PlanIdAndPpriceActiveTrue(plan.getPlanId());

        String display = Optional.ofNullable(plan.getPlanName())
                .map(PlanNameEntity::getPlanName)
                .filter(s -> !s.isBlank())
                .orElse(plan.getPlanCode());

        List<PlanDetailDTO.PlanPriceItem> items = prices.stream()
                .sorted(Comparator.comparing(pp -> pp.getPterm() != null ? pp.getPterm().getPtermMonth() : 9999))
                .map((PlanPriceEntity pp) -> {
                    var q = pricingService.quoteByPriceId(plan.getPlanId(), pp.getPpriceId());
                    var item = new PlanDetailDTO.PlanPriceItem();
                    item.setPpriceId(pp.getPpriceId());
                    item.setMonths(q.months());
                    item.setAmountInt(q.amountKRW());
                    item.setDiscountRate(q.discountRate());
                    return item;
                })
                .toList();

        PlanDetailDTO dto = PlanDetailDTO.builder()
                .displayName(display)
                .planCode(plan.getPlanCode())
                .rentalPriceCapInt((benefit != null && benefit.getPbPriceCap()!=null) ? benefit.getPbPriceCap().longValue() : 0L)
                .note(benefit != null ? benefit.getPbNote() : null)
                .isActive(Boolean.TRUE.equals(plan.getPlanActive()))
                .planPrices(items)
                .benefits((benefit != null && benefit.getPbNote()!=null) ? List.of(benefit.getPbNote()) : List.of())
                .build();

        return ResponseEntity.ok(dto);
    }
}
