package com.dodam.plan.controller;

import com.dodam.plan.dto.PlanCardDTO;
import com.dodam.plan.service.PlanBillingKeyService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/billing-keys")
public class PlanBillingKeyController {

    private final PlanBillingKeyService billingSvc;

    /**
     * 빌링키 등록
     * (프론트에서 billingKey, customerKey 전달)
     * AuthenticationPrincipal이 String일 경우 expression 제거 필요
     */
    @PostMapping("/register")
    public ResponseEntity<List<PlanCardDTO>> register(
            @RequestBody RegisterReq req,
            @AuthenticationPrincipal String mid
    ) {
        String billingKey = req.getBillingKey();
        String customerKey = (req.getCustomerKey() != null && !req.getCustomerKey().isBlank())
                ? req.getCustomerKey()
                : mid;

        billingSvc.registerAndSave(mid, customerKey, billingKey);
        return ResponseEntity.ok(billingSvc.listCards(mid));
    }

    /**
     * 등록된 카드 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<PlanCardDTO>> list(@AuthenticationPrincipal String mid) {
        return ResponseEntity.ok(billingSvc.listCards(mid));
    }

    @Data
    public static class RegisterReq {
        private String billingKey;
        private String customerKey;
    }
}
