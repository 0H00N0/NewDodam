// src/main/java/com/dodam/plan/service/PlanPaySubService.java
package com.dodam.plan.service;

import com.dodam.member.entity.MemberEntity;
import com.dodam.member.repository.MemberRepository;
import com.dodam.plan.Entity.PlanInvoiceEntity;
import com.dodam.plan.Entity.PlanMember;
import com.dodam.plan.Entity.PlanPriceEntity;
import com.dodam.plan.Entity.PlansEntity;
import com.dodam.plan.enums.PlanEnums.PiStatus;
import com.dodam.plan.enums.PlanEnums.PmBillingMode;
import com.dodam.plan.enums.PlanEnums.PmStatus;
import com.dodam.plan.repository.PlanInvoiceRepository;
import com.dodam.plan.repository.PlanMemberRepository;
import com.dodam.plan.repository.PlanPriceRepository;
import com.dodam.plan.repository.PlansRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanPaySubService {

    private final PlansRepository plansRepo;
    private final PlanMemberRepository planMemberRepo;
    private final PlanInvoiceRepository invoiceRepo;
    private final MemberRepository memberRepo;
    private final PlanPriceRepository priceRepo;

    public record StartResult(Long pmId, Long invoiceId) {}

    @Transactional
    public StartResult startByCodeAndMonths(String mid, String planCode, int months, PmBillingMode mode) {
        MemberEntity member = memberRepo.findByMid(mid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "member not found"));
        PlansEntity plan = plansRepo.findByPlanCodeEqualsIgnoreCase(planCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "plan not found"));
        Long planId = plan.getPlanId();

        PlanPriceEntity price = priceRepo.findByPlanIdAndMonths(planId, months)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "price not found"));

        // 최신 PlanMember 있으면 재사용, 없으면 신규
        PlanMember pm = planMemberRepo.findTopByMember_MidOrderByPmIdDesc(mid).orElse(null);
        if (pm == null) {
            pm = new PlanMember();
            pm.setMember(member);
            pm.setPmStat(PmStatus.PENDING);
            pm.setPmBilMode(mode);
            pm = planMemberRepo.save(pm);
        } else {
            pm.setPmStat(PmStatus.PENDING);
            pm.setPmBilMode(mode);
            pm = planMemberRepo.save(pm);
        }

        // 인보이스 생성
        LocalDateTime now = LocalDateTime.now();
        PlanInvoiceEntity inv = new PlanInvoiceEntity();
        inv.setPlanMember(pm);
        inv.setPiStart(now);
        inv.setPiEnd(now.plusMonths(Math.max(1, months)));
        inv.setPiAmount(price.getPpriceAmount() != null ? price.getPpriceAmount() : BigDecimal.ZERO);
        inv.setPiCurr(price.getPpriceCurr() != null ? price.getPpriceCurr() : "KRW");
        inv.setPiStat(PiStatus.PENDING);
        inv.setPiUid("pay_" + System.currentTimeMillis()); // 프론트가 이 paymentId로 승인 진행
        invoiceRepo.save(inv);

        return new StartResult(pm.getPmId(), inv.getPiId());
    }
}
