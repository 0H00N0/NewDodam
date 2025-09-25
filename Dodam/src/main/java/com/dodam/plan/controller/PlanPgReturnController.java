// src/main/java/com/dodam/plan/controller/PlanPgReturnController.java
package com.dodam.plan.controller;

import com.dodam.plan.dto.PlanPaymentRegisterReq;
import com.dodam.plan.service.PlanPaymentProfileService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/pg")
public class PlanPgReturnController {

    private final PlanPaymentProfileService profileSvc;

    /** PG 리다이렉트 복귀 후 카드정보 저장용 (프론트에서 전달된 값 사용) */
    @PostMapping("/return")
    public ResponseEntity<?> handleReturn(@RequestBody Map<String, Object> body, HttpSession session) {
        String mid = (String) session.getAttribute("sid");
        if (!StringUtils.hasText(mid)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error","LOGIN_REQUIRED"));
        }
        String paymentId = String.valueOf(body.getOrDefault("paymentId",""));
        String billingKey = String.valueOf(body.getOrDefault("billingKey",""));
        String pg = String.valueOf(body.getOrDefault("pg",""));
        String brand = String.valueOf(body.getOrDefault("brand",""));
        String bin = String.valueOf(body.getOrDefault("bin",""));
        String last4 = String.valueOf(body.getOrDefault("last4",""));
        String raw = String.valueOf(body.getOrDefault("rawJson",""));

        if (!StringUtils.hasText(billingKey)) {
            return ResponseEntity.badRequest().body(Map.of("error","MISSING_BILLING_KEY"));
        }
        var saved = profileSvc.upsert(mid, new PlanPaymentRegisterReq(
                "cust_" + mid, billingKey, pg, brand, bin, last4, raw
        ));
        log.info("PG return saved paymentId={}, mid={}, card={}/{} last4={}",
                paymentId, mid, brand, bin, last4);
        return ResponseEntity.ok(Map.of(
                "result","ok",
                "mid", saved.getMid(),
                "pg", saved.getPayPg(),
                "brand", saved.getPayBrand(),
                "bin", saved.getPayBin(),
                "last4", saved.getPayLast4(),
                "billingKey", saved.getPayKey()
        ));
    }
}
