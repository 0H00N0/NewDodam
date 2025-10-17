package com.dodam.plan.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;

import com.dodam.discount.repository.DiscountRepository;
import com.dodam.plan.Entity.PlanPriceEntity;
import com.dodam.plan.repository.PlanPriceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlanPriceService {

    private final PlanPriceRepository priceRepo;
    private final DiscountRepository discountRepo;

    public record Quote(Long planId, Long ppriceId, Integer months, int discountRate, long amountKRW) {}

    /** ppriceId 기준(기간/빌링모드가 명확) */
    public Quote quoteByPriceId(Long planId, Long ppriceId) {
        var pp = priceRepo.findById(ppriceId)
                .orElseThrow(() -> new IllegalArgumentException("가격 항목을 찾을 수 없습니다: " + ppriceId));

        if (pp.getPlan() == null || !pp.getPlan().getPlanId().equals(planId)) {
            throw new IllegalArgumentException("planId와 ppriceId가 매칭되지 않습니다.");
        }

        Integer months = (pp.getPterm() != null) ? pp.getPterm().getPtermMonth() : 1;

        // 1개월 기준가
        BigDecimal oneMonth = priceRepo.findActiveByPlanIdAndMonths(planId, 1)
                .map(PlanPriceEntity::getPpriceAmount)
                .orElse(pp.getPpriceAmount());

        // 할인율: ptermId 우선 → 없으면 months
        int rate = 0;
        if (pp.getPterm() != null) {
            rate = discountRepo.findRateByPterm(pp.getPterm().getPtermId())
                    .or(() -> discountRepo.findRateByMonths(months))
                    .orElse(0);
        } else {
            rate = discountRepo.findRateByMonths(months).orElse(0);
        }

        BigDecimal amount = (months == 1)
                ? pp.getPpriceAmount()
                : oneMonth.multiply(BigDecimal.valueOf(months))
                         .multiply(BigDecimal.valueOf(100 - rate))
                         .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);

        return new Quote(planId, ppriceId, months, rate, amount.longValue());
    }

    /** planId + months 기준 (가격 엔티티 선택 없이 금액만 산출) */
    public Quote quoteByPlanAndMonths(Long planId, int months) {
        if (months <= 0) months = 1;

        BigDecimal oneMonth = priceRepo.findActiveByPlanIdAndMonths(planId, 1)
                .map(PlanPriceEntity::getPpriceAmount)
                .orElseThrow(() -> new IllegalStateException("1개월 기준가가 없습니다. planId=" + planId));

        int rate = discountRepo.findRateByMonths(months).orElse(0);

        BigDecimal amount = (months == 1)
                ? oneMonth
                : oneMonth.multiply(BigDecimal.valueOf(months))
                          .multiply(BigDecimal.valueOf(100 - rate))
                          .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);

        return new Quote(planId, null, months, rate, amount.longValue());
    }
}
