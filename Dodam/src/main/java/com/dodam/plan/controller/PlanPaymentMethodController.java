package com.dodam.plan.controller;

import com.dodam.plan.Entity.PlanPaymentEntity;
import com.dodam.plan.repository.PlanPaymentRepository;
import com.dodam.plan.service.PlanPaymentProfileService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/billing-keys")
public class PlanPaymentMethodController {

    private final PlanPaymentProfileService profileSvc;
    private final PlanPaymentRepository paymentRepo;

    /** 카드 목록 (최신순) */
    @GetMapping("/list")
    public ResponseEntity<?> listCards(HttpSession session) {
        String mid = resolveMid(session);
        if (!StringUtils.hasText(mid)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error","LOGIN_REQUIRED"));
        }
        List<Map<String,Object>> list = profileSvc.listCards(mid);
        if (list.isEmpty()) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(list);
    }

    /** 활성 카드 요약 조회 (최신 1건) */
    @GetMapping("/active")
    public ResponseEntity<?> getActiveCard(HttpSession session) {
        String mid = resolveMid(session);
        if (!StringUtils.hasText(mid)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error","LOGIN_REQUIRED"));
        }
        return profileSvc.getActiveCardMeta(mid)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /** 카드 등록/교체 */
    @PostMapping(value = {"", "/change"}, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> registerOrChange(@RequestBody Map<String, String> req, HttpSession session) {
        String mid = resolveMid(session);
        if (!StringUtils.hasText(mid)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error","LOGIN_REQUIRED"));
        }

        String billingKey = req.get("billingKey");
        if (!StringUtils.hasText(billingKey)) {
            return ResponseEntity.badRequest().body(Map.of("error","MISSING_BILLING_KEY"));
        }

        String brand = trimOrNull(req.get("brand"));
        String bin   = trimOrNull(req.get("bin"));
        String last4 = trimOrNull(req.get("last4"));
        String pg    = trimOrNull(req.getOrDefault("pg", "TossPayments"));
        String raw   = trimOrNull(req.get("raw"));

        PlanPaymentEntity saved = profileSvc.changeCard(mid, billingKey, brand, bin, last4, pg, raw);
        return ResponseEntity.ok(Map.of(
                "payId", saved.getPayId(),
                "brand", saved.getPayBrand(),
                "last4", saved.getPayLast4(),
                "pg",    saved.getPayPg()
        ));
    }

    /** 활성 카드 삭제(=비활성화) */
    @DeleteMapping("/active")
    public ResponseEntity<?> removeActive(HttpSession session) {
        String mid = resolveMid(session);
        if (!StringUtils.hasText(mid)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error","LOGIN_REQUIRED"));
        }
        boolean ok = profileSvc.removeActive(mid);
        return ok ? ResponseEntity.noContent().build()
                  : ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error","NOT_FOUND"));
    }

    /* ===== utils ===== */
    private static String resolveMid(HttpSession session) {
        // 1) SecurityContext 우선
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a != null && StringUtils.hasText(a.getName())) return a.getName();
        // 2) 세션 fallback(sid=mid, mnum=숫자 mid)
        Object sid = session != null ? session.getAttribute("sid") : null;
        if (sid instanceof String s && StringUtils.hasText(s)) return s;
        Object mnum = session != null ? session.getAttribute("mnum") : null;
        if (mnum instanceof Number n) return String.valueOf(n.longValue());
        return null;
    }
    private static String trimOrNull(String s){ return (s == null) ? null : s.trim(); }
}
