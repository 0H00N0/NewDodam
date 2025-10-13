package com.dodam.member.controller;

import com.dodam.member.repository.MemberRepository;
import com.dodam.plan.enums.PlanEnums.PmStatus;
import com.dodam.plan.repository.PlanMemberRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

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
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error","LOGIN_REQUIRED"));
        }

        var member = memberRepo.findByMid(mid).orElse(null);
        if (member == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error","MEMBER_NOT_FOUND"));
        }

        var now = LocalDateTime.now();

        var list = planMemberRepo.findAllByMember_Mnum(member.getMnum());

        List<Map<String,Object>> result = list.stream()
            // 최신 먼저 보이도록 정렬 (선택)
            .sorted(Comparator.comparingLong(pm -> -pm.getPmId()))
            .map(pm -> {
                Map<String,Object> m = new LinkedHashMap<>();
                m.put("pmId", pm.getPmId());

                // ✅ 파생 상태 계산
                PmStatus raw = pm.getPmStatus();
                boolean scheduled = Boolean.TRUE.equals(pm.isCancelAtPeriodEnd())
                        && pm.getPmTermEnd() != null
                        && now.isBefore(pm.getPmTermEnd());

                PmStatus eff;
                if (raw == PmStatus.CANCEL_SCHEDULED || scheduled) {
                    eff = PmStatus.CANCEL_SCHEDULED;
                } else if (raw == null) {
                    // 상태가 null이면 기간으로 추정
                    if (pm.getPmTermEnd() != null && !now.isBefore(pm.getPmTermEnd())) {
                        // ENDED -> CANCELED 로 변경
                        eff = PmStatus.CANCELED;
                    } else {
                        eff = PmStatus.ACTIVE;
                    }
                } else {
                    // 기간이 이미 지났으면 CANCELED로 보정
                    if (pm.getPmTermEnd() != null && !now.isBefore(pm.getPmTermEnd())) {
                        eff = PmStatus.CANCELED;
                    } else {
                        eff = raw;
                    }
                }
                m.put("status", eff.name());

                m.put("billingMode", pm.getPmBilMode() != null ? pm.getPmBilMode().name() : null);

                if (pm.getPlan()!=null) {
                    m.put("planCode", pm.getPlan().getPlanCode());
                    m.put("planName",
                        pm.getPlan().getPlanName() != null ? pm.getPlan().getPlanName().getPlanName() : null
                    );
                } else {
                    m.put("planCode", null);
                    m.put("planName", null);
                }

                Integer termMonth = pm.getTerms()!=null ? pm.getTerms().getPtermMonth() : null;
                m.put("termLabel", termMonth != null ? termMonth + "개월" : null);
                m.put("termMonth", termMonth);

                m.put("termStart", pm.getPmTermStart());
                m.put("termEnd", pm.getPmTermEnd());
                m.put("nextBillingAt", pm.getPmNextBil());

                if (pm.getPayment()!=null) {
                    m.put("cardBrand", pm.getPayment().getPayBrand());
                    m.put("cardLast4", pm.getPayment().getPayLast4());
                } else {
                    m.put("cardBrand", null);
                    m.put("cardLast4", null);
                }

                return m;
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}
