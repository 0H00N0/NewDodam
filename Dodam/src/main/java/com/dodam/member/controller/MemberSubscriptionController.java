package com.dodam.member.controller;

import com.dodam.member.repository.MemberRepository;
import com.dodam.plan.repository.PlanMemberRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/member/subscriptions")   // ✅ 베이스를 subscriptions로 고정
public class MemberSubscriptionController {

    private final MemberRepository memberRepo;
    private final PlanMemberRepository planMemberRepo;

    @GetMapping("/my") // ✅ 결과적으로 /member/subscriptions/my
    public ResponseEntity<?> my(HttpSession session) {
        String mid = (String) session.getAttribute("sid");
        if (mid == null || mid.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error","LOGIN_REQUIRED"));
        }

        var member = memberRepo.findByMid(mid).orElse(null);
        if (member == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error","MEMBER_NOT_FOUND"));
        }

        var list = planMemberRepo.findAllByMember_Mnum(member.getMnum());

        List<Map<String,Object>> result = list.stream().map(pm -> {
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("pmId", pm.getPmId());
            m.put("status", pm.getPmStat() != null ? pm.getPmStat().name() : null);
            m.put("billingMode", pm.getPmBilMode() != null ? pm.getPmBilMode().name() : null);

            if (pm.getPlan()!=null) {
                m.put("planCode", pm.getPlan().getPlanCode());
                m.put("planName",
                	    pm.getPlan() != null && pm.getPlan().getPlanName() != null
                	        ? pm.getPlan().getPlanName().getPlanName()   // ✅ 문자열만 담기
                	        : null
                	);
            } else { m.put("planCode", null); m.put("planName", null); }

            Integer termMonth = pm.getTerms()!=null ? pm.getTerms().getPtermMonth() : null;
            m.put("termLabel", termMonth != null ? termMonth + "개월" : null);
            m.put("termMonth", termMonth);

            m.put("termStart", pm.getPmTermStart());
            m.put("termEnd", pm.getPmTermEnd());
            m.put("nextBillingAt", pm.getPmNextBil());

            if (pm.getPayment()!=null) {
                m.put("cardBrand", pm.getPayment().getPayBrand());
                m.put("cardLast4", pm.getPayment().getPayLast4());
            } else { m.put("cardBrand", null); m.put("cardLast4", null); }

            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

   
}
