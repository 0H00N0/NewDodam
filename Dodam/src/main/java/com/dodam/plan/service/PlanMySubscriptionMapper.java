// src/main/java/com/dodam/plan/service/mapper/SubscriptionMapper.java
package com.dodam.plan.service;

import com.dodam.plan.Entity.PlanMember;
import com.dodam.plan.Entity.PlanPriceEntity;
import com.dodam.plan.Entity.PlanTermsEntity;
import com.dodam.plan.Entity.PlansEntity;
import com.dodam.plan.dto.PlanMySubscriptionDTO;
import com.dodam.plan.enums.PlanEnums;

import java.time.LocalDateTime;

public final class PlanMySubscriptionMapper {

    private PlanMySubscriptionMapper() {}

    public static PlanMySubscriptionDTO toDto(PlanMember pm) {
        // 현재 값
        PlansEntity     plan  = pm.getPlan();
        PlanTermsEntity terms = pm.getTerms();
        PlanPriceEntity price = pm.getPrice();

        // 예약(다음 주기) 값
        PlansEntity     nPlan  = pm.getNextPlan();
        PlanTermsEntity nTerms = pm.getNextTerms();
        PlanPriceEntity nPrice = pm.getNextPrice();

        // ---- 현재 구독 표시용 ----
        String planCode = (plan != null) ? plan.getPlanCode() : null;
        String planName = (plan != null && plan.getPlanName() != null)
                ? plan.getPlanName().getPlanName()
                : null;
        Integer months = (terms != null && terms.getPtermMonth() != null)
                ? terms.getPtermMonth()
                : pm.getPmCycle();

        String curr = (price != null) ? nz(price.getPpriceCurr(), "KRW") : "KRW";
        var amount  = (price != null) ? price.getPpriceAmount() : null;

        // ---- 변경 예약 여부 ----
        boolean scheduled = (nPlan != null) || (nTerms != null) || (nPrice != null);

        // ---- 다음 갱신 예정(변경 예약이 없다면 현재값 재사용) ----
        String  nPlanCode;
        String  nPlanName;
        Integer nMonths;
        String  nCurr;
        var     nAmount = (nPrice != null) ? nPrice.getPpriceAmount() : null;

        if (nPlan != null) {
            nPlanCode = nPlan.getPlanCode();
            nPlanName = (nPlan.getPlanName() != null) ? nPlan.getPlanName().getPlanName() : null;
        } else if (nPrice != null && nPrice.getPlan() != null) {
            nPlanCode = nPrice.getPlan().getPlanCode();
            nPlanName = (nPrice.getPlan().getPlanName() != null)
                    ? nPrice.getPlan().getPlanName().getPlanName()
                    : null;
        } else {
            nPlanCode = planCode;
            nPlanName = planName;
        }

        if (nTerms != null && nTerms.getPtermMonth() != null) {
            nMonths = nTerms.getPtermMonth();
        } else if (nPrice != null && nPrice.getPterm() != null && nPrice.getPterm().getPtermMonth() != null) {
            nMonths = nPrice.getPterm().getPtermMonth();
        } else {
            nMonths = months;
        }

        nCurr = (nPrice != null) ? nz(nPrice.getPpriceCurr(), "KRW") : curr;

        // ---- 다음 이용기간 계산 ----
        LocalDateTime nextTermStart = pm.getPmNextBil(); // 보통 다음 주기 시작일
        LocalDateTime nextTermEnd   = null;
        if (nextTermStart != null && nMonths != null) {
            nextTermEnd = nextTermStart.plusMonths(nMonths);
        }

        return new PlanMySubscriptionDTO(
                pm.getPmId(),
                pm.getPmStatus() != null ? pm.getPmStatus().name() : PlanEnums.PmStatus.PENDING.name(),
                planCode, planName, months, amount, curr,
                pm.getPmTermStart(),
                pm.getPmTermEnd(),
                pm.getPmNextBil(),     // nextBillingAt (하위 호환)
                nextTermStart,         // ✅ 추가
                nextTermEnd,           // ✅ 추가
                scheduled,
                nPlanCode, nPlanName, nMonths, nAmount, nCurr
        );
    }

    private static String nz(String v, String dft) {
        return (v == null || v.isBlank()) ? dft : v;
    }
}
