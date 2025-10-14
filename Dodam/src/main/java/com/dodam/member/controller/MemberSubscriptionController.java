package com.dodam.member.controller;

import com.dodam.member.repository.MemberRepository;
import com.dodam.plan.Entity.PlanMember;
import com.dodam.plan.Entity.PlanPriceEntity;
import com.dodam.plan.Entity.PlanTermsEntity;
import com.dodam.plan.enums.PlanEnums.PmStatus;
import com.dodam.plan.repository.PlanMemberRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/member/subscriptions")
public class MemberSubscriptionController {

    private final MemberRepository memberRepo;
    private final PlanMemberRepository planMemberRepo;

    @GetMapping("/my")
    public ResponseEntity<?> my(HttpSession session) {
        String mid = (String) session.getAttribute("sid");
        if (mid == null || mid.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "LOGIN_REQUIRED"));
        }

        var member = memberRepo.findByMid(mid).orElse(null);
        if (member == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "MEMBER_NOT_FOUND"));
        }

        var now = LocalDateTime.now();

        var list = planMemberRepo.findAllByMember_Mnum(member.getMnum());

        List<Map<String, Object>> result = list.stream()
                .sorted(Comparator.comparingLong(pm -> -pm.getPmId())) // 최신 먼저
                .map(pm -> toView(pm, now))
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /* ================= helpers ================= */

    private Map<String, Object> toView(PlanMember pm, LocalDateTime now) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("pmId", pm.getPmId());

        // —— 파생 상태(해지예약 보정 포함)
        PmStatus raw = pm.getPmStatus();
        boolean periodEndFuture = pm.getPmTermEnd() != null && now.isBefore(pm.getPmTermEnd());
        boolean cancelScheduled = Boolean.TRUE.equals(pm.isCancelAtPeriodEnd()) && periodEndFuture;

        PmStatus eff;
        if (raw == PmStatus.CANCEL_SCHEDULED || cancelScheduled) {
            eff = PmStatus.CANCEL_SCHEDULED;
        } else if (raw == null) {
            if (pm.getPmTermEnd() != null && !now.isBefore(pm.getPmTermEnd())) eff = PmStatus.CANCELED;
            else eff = PmStatus.ACTIVE;
        } else {
            if (pm.getPmTermEnd() != null && !now.isBefore(pm.getPmTermEnd())) eff = PmStatus.CANCELED;
            else eff = raw;
        }
        m.put("status", eff.name());

        m.put("billingMode", pm.getPmBilMode() != null ? pm.getPmBilMode().name() : null);

        // —— 현재 플랜 표시
        if (pm.getPlan() != null) {
            m.put("planCode", pm.getPlan().getPlanCode());
            m.put("planName", pm.getPlan().getPlanName() != null
                    ? pm.getPlan().getPlanName().getPlanName()
                    : null);
        } else {
            m.put("planCode", null);
            m.put("planName", null);
        }

        Integer termMonth = monthOf(pm.getTerms());
        m.put("termLabel", termMonth != null ? termMonth + "개월" : null);
        m.put("termMonth", termMonth);

        m.put("termStart", pm.getPmTermStart());
        m.put("termEnd", pm.getPmTermEnd());
        m.put("nextBillingAt", pm.getPmNextBil());

        if (pm.getPayment() != null) {
            m.put("cardBrand", pm.getPayment().getPayBrand());
            m.put("cardLast4", pm.getPayment().getPayLast4());
        } else {
            m.put("cardBrand", null);
            m.put("cardLast4", null);
        }

        // ===== 다음 갱신 예정(변경 예약 포함) =====
        // PlanMember 엔티티에 next*Id 필드가 없으므로, 객체 참조로만 판단
        boolean hasScheduledChange =
                pm.getNextPlan() != null || pm.getNextTerms() != null || pm.getNextPrice() != null;

        // 다음 주기 기준 플랜/개월/금액 (변경 예약 있으면 next*, 없으면 현재값)
        String nextPlanCode;
        String nextPlanName;
        Integer nextTermMonth;
        BigDecimal nextAmount;
        String nextCurrency;

        if (hasScheduledChange) {
            // next* 우선
            if (pm.getNextPlan() != null) {
                nextPlanCode = pm.getNextPlan().getPlanCode();
                nextPlanName = pm.getNextPlan().getPlanName() != null
                        ? pm.getNextPlan().getPlanName().getPlanName()
                        : null;
            } else if (pm.getNextPrice() != null && pm.getNextPrice().getPlan() != null) {
                nextPlanCode = pm.getNextPrice().getPlan().getPlanCode();
                nextPlanName = pm.getNextPrice().getPlan().getPlanName() != null
                        ? pm.getNextPrice().getPlan().getPlanName().getPlanName()
                        : null;
            } else if (pm.getPlan() != null) {
                nextPlanCode = pm.getPlan().getPlanCode();
                nextPlanName = pm.getPlan().getPlanName() != null
                        ? pm.getPlan().getPlanName().getPlanName()
                        : null;
            } else {
                nextPlanCode = null;
                nextPlanName = null;
            }

            nextTermMonth = monthOf(pm.getNextTerms());
            if (nextTermMonth == null && pm.getNextPrice() != null && pm.getNextPrice().getPterm() != null) {
                nextTermMonth = monthOf(pm.getNextPrice().getPterm()); // <- getMonths 사용 금지, getPtermMonth로
            }
            if (nextTermMonth == null) nextTermMonth = termMonth; // fallback

            PlanPriceEntity np = pm.getNextPrice();
            nextAmount = (np != null ? np.getPpriceAmount() : (pm.getPrice() != null ? pm.getPrice().getPpriceAmount() : null));
            nextCurrency = (np != null ? nz(np.getPpriceCurr(), "KRW")
                    : (pm.getPrice() != null ? nz(pm.getPrice().getPpriceCurr(), "KRW") : "KRW"));

        } else {
            // 변경 예약이 없을 때: 현재값을 그대로 “다음 예정”으로
            if (pm.getPlan() != null) {
                nextPlanCode = pm.getPlan().getPlanCode();
                nextPlanName = pm.getPlan().getPlanName() != null
                        ? pm.getPlan().getPlanName().getPlanName()
                        : null;
            } else {
                nextPlanCode = null;
                nextPlanName = null;
            }
            nextTermMonth = termMonth;
            nextAmount = pm.getPrice() != null ? pm.getPrice().getPpriceAmount() : null;
            nextCurrency = pm.getPrice() != null ? nz(pm.getPrice().getPpriceCurr(), "KRW") : "KRW";
        }

        m.put("hasScheduledChange", hasScheduledChange);
        m.put("nextPlanCode", nextPlanCode);
        m.put("nextPlanName", nextPlanName);
        m.put("nextTermMonth", nextTermMonth);
        m.put("nextAmount", nextAmount);
        m.put("nextCurrency", nextCurrency);

        return m;
    }

    private Integer monthOf(PlanTermsEntity t) {
        if (t == null) return null;
        Integer m = t.getPtermMonth();   // 프로젝트 스키마: getPtermMonth 사용
        return (m != null && m > 0) ? m : null;
    }

    private String nz(String v, String dft) {
        return (v == null || v.isBlank()) ? dft : v;
    }
}
