package com.dodam.member.controller;

import com.dodam.member.repository.MemberRepository;
import com.dodam.plan.repository.PlanMemberRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/member")
public class MemberSubscriptionController {

    private final MemberRepository memberRepo;
    private final PlanMemberRepository planMemberRepo;

    /** 내 구독 목록 */
    @GetMapping("/subscriptions/my")
    public ResponseEntity<?> mySubscriptions(HttpSession session) {
        final String mid = (String) session.getAttribute("sid");
        if (!StringUtils.hasText(mid)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "LOGIN_REQUIRED"));
        }

        var member = memberRepo.findByMid(mid).orElse(null);
        if (member == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "MEMBER_NOT_FOUND"));
        }

        var list = planMemberRepo.findAllByMember_Mnum(member.getMnum());

        List<Map<String, Object>> result = list.stream()
        	    .map(pm -> {
        	        Map<String, Object> m = new LinkedHashMap<>(); // 순서 유지
        	        m.put("pmId",          pm.getPmId());
        	        m.put("status",        pm.getPmStat() != null ? pm.getPmStat().name() : null);
        	        m.put("billingMode",   pm.getPmBilMode() != null ? pm.getPmBilMode().name() : null);

        	        // plan
        	        if (pm.getPlan() != null) {
        	            m.put("planCode", pm.getPlan().getPlanCode());
        	            m.put("planName", pm.getPlan().getPlanName());
        	        } else {
        	            m.put("planCode", null);
        	            m.put("planName", null);
        	        }

        	        // terms
        	        Integer termMonth = (pm.getTerms() != null) ? pm.getTerms().getPtermMonth() : null;
        	        m.put("termLabel", termMonth != null ? termMonth + "개월" : null);  // 사람이 읽기 좋은 라벨
        	        m.put("termMonth", termMonth);                                      // 숫자값도 함께 내려줌(선택)
        	        m.put("termStart",     pm.getPmTermStart());
        	        m.put("termEnd",       pm.getPmTermEnd());
        	        m.put("nextBillingAt", pm.getPmNextBil());

        	        // payment
        	        if (pm.getPayment() != null) {
        	            m.put("cardBrand", pm.getPayment().getPayBrand());
        	            m.put("cardLast4", pm.getPayment().getPayLast4());
        	        } else {
        	            m.put("cardBrand", null);
        	            m.put("cardLast4", null);
        	        }
        	        return m;
        	    })
        	    .collect(Collectors.toList()); // JDK 버전 상관없이 안전

        return ResponseEntity.ok(result);
    }
}
