// src/main/java/com/dodam/plan/controller/PlanPgProxyController.java
package com.dodam.plan.controller;

import com.dodam.plan.service.PlanPaymentGatewayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/pg")
public class PlanPgProxyController {
    private final PlanPaymentGatewayService pgSvc;

    @GetMapping(value = "/lookup", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> lookup(@RequestParam(required = false) String txId,
                                    @RequestParam(required = false) String paymentId) {
        return ResponseEntity.ok(pgSvc.safeLookup(paymentId));
    }
}
