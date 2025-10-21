package com.dodam.plan.controller;

import com.dodam.plan.Entity.PlanInvoiceEntity;
import com.dodam.plan.Entity.PlanMember;
import com.dodam.plan.enums.PlanEnums.PiStatus;
import com.dodam.plan.repository.PlanInvoiceRepository;
import com.dodam.plan.repository.PlanAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/plan/checkout")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
@Slf4j
public class PlanCheckoutSummaryController {

    private final PlanInvoiceRepository invoiceRepo;
    private final PlanAttemptRepository attemptRepo;

    @GetMapping("/summary")
    public ResponseEntity<?> summary(@RequestParam Map<String,String> params) {
        // ✅ 자동타입변환 완전 회피: 전부 String으로 받고 직접 파싱
        final String orderId   = trim(params.get("orderId"));
        final String paymentId = trim(params.get("paymentId"));
        final String invoiceId = trim(params.get("invoiceId"));

        PlanInvoiceEntity inv = null;

        // 0) UID 정확 일치 (대소문자/공백 무시)
        String uid = firstNonBlank(orderId, paymentId);
        if (StringUtils.hasText(uid)) {
            inv = invoiceRepo.findByPiUidInsensitive(uid).orElse(null);
            if (inv == null && uid.toLowerCase().startsWith("inv")) {
                // prefix(inv###) 로 보조
                String prefix = uid.split("-")[0]; // inv235
                inv = invoiceRepo.findTopByPiUidStartsWith(prefix).orElse(null);
            }
        }

        // 1) paymentId → PLANATTEMPT 에서 PIID 역추적
        if (inv == null && StringUtils.hasText(paymentId)) {
            var piid = attemptRepo.findInvoiceIdByPaymentUid(paymentId).orElse(null);
            if (piid != null) inv = invoiceRepo.findById(piid).orElse(null);
        }

        // 2) invoiceId 문자열에서 숫자 뽑기
        Long numericInvoiceId = parseDigitsToLong(invoiceId);
        if (inv == null && numericInvoiceId != null) {
            inv = invoiceRepo.findById(numericInvoiceId).orElse(null);
        }

        if (inv == null) {
            return ResponseEntity.ok(Map.of(
                "status", "UNKNOWN",
                "message", "Invoice not found",
                "echo", Map.of("orderId", orderId, "paymentId", paymentId, "invoiceId", invoiceId)
            ));
        }

        // ===== 응답 구성 =====
        Map<String,Object> res = new LinkedHashMap<>();
        res.put("invoiceId", inv.getPiId());
        res.put("status", inv.getPiStat() != null ? inv.getPiStat().name() : PiStatus.PENDING.name());
        res.put("amount", inv.getPiAmount());
        res.put("currency", StringUtils.hasText(inv.getPiCurr()) ? inv.getPiCurr() : "KRW");

        // Plan/Months
        String planCode = null, planName = "구독 플랜";
        Integer months = null;

        PlanMember pm = inv.getPlanMember();
        if (pm != null && pm.getPlan() != null) {
            planCode = pm.getPlan().getPlanCode();
            if (pm.getPlan().getPlanName() != null && pm.getPlan().getPlanName().getPlanName() != null) {
                planName = pm.getPlan().getPlanName().getPlanName();
            } else if (StringUtils.hasText(planCode)) {
                planName = planCode;
            }
            months = (pm.getPmCycle() != null && pm.getPmCycle() > 0) ? pm.getPmCycle() : 1;
        } else {
            months = 1;
        }

        if (planCode != null) res.put("planCode", planCode);
        res.put("planName", planName);
        res.put("months", months);

        // 🔗 최신 성공건 영수증 URL
        attemptRepo.findLatestReceiptUrlByInvoiceId(inv.getPiId())
                .ifPresent(u -> res.put("receiptUrl", u));

        return ResponseEntity.ok(res);
    }

    // ===== helpers =====
    private static String trim(String s) { return s == null ? null : s.trim(); }

    private static String firstNonBlank(String... arr) {
        if (arr == null) return null;
        for (String s : arr) if (StringUtils.hasText(s)) return s;
        return null;
    }

    private static Long parseDigitsToLong(String raw) {
        if (!StringUtils.hasText(raw)) return null;
        try {
            String digits = raw.replaceAll("[^0-9]", "");
            if (digits.isEmpty()) return null;
            return Long.parseLong(digits);
        } catch (Exception e) { return null; }
    }
}
