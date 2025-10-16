package com.dodam.plan.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.dodam.discount.repository.DiscountRepository;
import com.dodam.plan.Entity.PlanNameEntity;
import com.dodam.plan.Entity.PlanPriceEntity;
import com.dodam.plan.Entity.PlansEntity;
import com.dodam.plan.dto.PlanDTO;
import com.dodam.plan.dto.PlanDetailDTO;
import com.dodam.plan.repository.PlanBenefitRepository;
import com.dodam.plan.repository.PlanPriceRepository;
import com.dodam.plan.repository.PlansRepository;
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
    private final DiscountRepository discountRepo;

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

        // 표시명(PlanNameEntity -> String)
        String display = Optional.ofNullable(plan.getPlanName())
                .map(PlanNameEntity::getPlanName)
                .filter(s -> !s.isBlank())
                .orElse(plan.getPlanCode());

        // 1개월 기준가
        BigDecimal oneMonth = ppr.findActiveByPlanIdAndMonths(plan.getPlanId(), 1)
                .map(PlanPriceEntity::getPpriceAmount)
                .orElse(null);

        // 기간별 항목(할인율/총액 포함) + ppriceId 포함
        List<PlanDetailDTO.PlanPriceItem> items = prices.stream()
                .sorted(Comparator.comparing(pp -> pp.getPterm() != null ? pp.getPterm().getPtermMonth() : 9999))
                .map(pp -> {
                    Integer months = (pp.getPterm() != null) ? pp.getPterm().getPtermMonth() : null;
                    Long ptermId   = (pp.getPterm() != null) ? pp.getPterm().getPtermId()   : null;

                    // 할인율: ptermId 우선 → 없으면 months 보조
                    int rate = 0;
                    if (ptermId != null) {
                        rate = discountRepo.findRateByPterm(ptermId)
                                .or(() -> months != null ? discountRepo.findRateByMonths(months) : Optional.empty())
                                .orElse(0);
                    } else if (months != null) {
                        rate = discountRepo.findRateByMonths(months).orElse(0);
                    }

                    // 총액 계산
                    BigDecimal amount;
                    if (months != null && months == 1) {
                        amount = pp.getPpriceAmount();
                    } else if (oneMonth != null && months != null) {
                        BigDecimal gross = oneMonth.multiply(BigDecimal.valueOf(months));
                        amount = gross.multiply(BigDecimal.valueOf(100 - rate))
                                      .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
                    } else {
                        amount = pp.getPpriceAmount();
                    }

                    // 컨트롤러는 new/setter 패턴을 사용 → DTO에 NoArgsConstructor/Setter 추가해 둠
                    PlanDetailDTO.PlanPriceItem item = new PlanDetailDTO.PlanPriceItem();
                    item.setPpriceId(pp.getPpriceId());
                    item.setMonths(months);
                    item.setAmountInt(amount != null ? amount.longValue() : 0L);
                    item.setDiscountRate(rate);
                    return item;
                })
                .toList();

        PlanDetailDTO dto = PlanDetailDTO.builder()
                .displayName(display)
                .planCode(plan.getPlanCode())
                .rentalPriceCapInt(
                        (benefit != null && benefit.getPbPriceCap()!=null)
                                ? benefit.getPbPriceCap().longValue()
                                : 0L
                )
                .note(benefit != null ? benefit.getPbNote() : null)
                .isActive(Boolean.TRUE.equals(plan.getPlanActive()))
                .planPrices(items)
                // 프론트가 리스트를 기대하므로 pbNote라도 리스트로 제공
                .benefits(
                        (benefit != null && benefit.getPbNote()!=null)
                                ? List.of(benefit.getPbNote())
                                : List.of()
                )
                .build();

        return ResponseEntity.ok(dto);
    }
}
