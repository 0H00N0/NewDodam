// src/main/java/com/dodam/plan/controller/PlanBillingKeyController.java
package com.dodam.plan.controller;

import com.dodam.plan.dto.PlanCardDTO;
import com.dodam.plan.service.PlanBillingKeyService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/billing-keys")
public class PlanBillingKeyController {

    private final PlanBillingKeyService billingSvc;

    /**
     * ✅ 빌링키 등록
     */
    @PostMapping("/register")
    public ResponseEntity<List<PlanCardDTO>> register(
            @RequestBody RegisterReq req,
            Authentication auth
    ) {
        String mid = auth != null ? auth.getName() : null;
        billingSvc.registerAndSave(
                mid,
                (req.getCustomerKey() != null && !req.getCustomerKey().isBlank())
                        ? req.getCustomerKey()
                        : mid,
                req.getBillingKey()
        );
        return ResponseEntity.ok(billingSvc.listCards(mid));
    }

    /**
     * ✅ 카드 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<PlanCardDTO>> list(Authentication auth) {
        String mid = auth != null ? auth.getName() : null;
        return ResponseEntity.ok(billingSvc.listCards(mid));
    }

    /**
     * ✅ payId 기반 비활성화 (삭제와 동일)
     * - 항상 204 반환 (idempotent)
     */
    @DeleteMapping("/by-id/{payId}")
    public ResponseEntity<Void> deleteById(
            @PathVariable("payId") Long payId,  // ✅ 이름 명시
            Authentication auth
    ) {
        String mid = auth != null ? auth.getName() : null;
        try {
            billingSvc.deactivateById(mid, payId);
        } catch (Exception e) {
            log.warn("[BillingKey] deleteById ignored: {}", e.toString());
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * ✅ billingKey 기반 비활성화 (삭제와 동일)
     * - 항상 204 반환 (idempotent)
     */
    @DeleteMapping("/{payKey}")
    public ResponseEntity<Void> deleteByKey(
            @PathVariable("payKey") String payKey, // ✅ 이름 명시
            Authentication auth
    ) {
        String mid = auth != null ? auth.getName() : null;
        try {
            billingSvc.deactivateByKey(mid, payKey);
        } catch (Exception e) {
            log.warn("[BillingKey] deleteByKey ignored: {}", e.toString());
        }
        return ResponseEntity.noContent().build();
    }

    @Data
    public static class RegisterReq {
        private String billingKey;
        private String customerKey;
    }
}
