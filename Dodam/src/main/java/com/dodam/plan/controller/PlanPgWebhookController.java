// src/main/java/com/dodam/plan/controller/PlanPgWebhookController.java
package com.dodam.plan.controller;

import com.dodam.plan.config.PlanPortoneProperties;
import com.dodam.plan.webhook.PlanWebhookProcessingService; // ← 네가 올린 서비스 그대로 사용
import io.portone.sdk.server.webhook.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/webhooks")
public class PlanPgWebhookController {

    private final PlanPortoneProperties props;                // portone.webhookSecret 사용
    private final PlanWebhookProcessingService processingSvc; // 네 서비스 (@Service, process(...))

    @PostMapping("/pg")
    public ResponseEntity<?> handlePortoneWebhook(
            HttpServletRequest request,
            @RequestBody(required = false) String rawBody
    ) {
        // 1) 헤더 맵 수집
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        while (names != null && names.hasMoreElements()) {
            String name = names.nextElement();
            headers.put(name, request.getHeader(name));
        }

        // 2) 서명 검증
        String secret = props.getWebhookSecret(); // 반드시 whsec_... 키 (API v2 secret와 다름)
        Webhook webhook;
        try {
            WebhookVerifier verifier = new WebhookVerifier(secret);
            webhook = verifier.verify(
                    rawBody == null ? "" : rawBody,
                    headers.get(WebhookVerifier.HEADER_ID),
                    headers.get(WebhookVerifier.HEADER_SIGNATURE),
                    headers.get(WebhookVerifier.HEADER_TIMESTAMP)
            );
        } catch (Exception ex) {
            log.warn("[WEBHOOK] verify failed: {}", ex.toString());
            return ResponseEntity.badRequest().body("invalid signature");
        }

        // 3) 이벤트 타입별로 분기하여 기존 서비스 호출 (PAID/FAILED/PENDING 등)
        try {
            if (webhook instanceof WebhookTransactionPaid paid) {
                var d = paid.getData(); // WebhookTransactionDataPaid
                String paymentId = safe(d.getPaymentId());
                String txId      = safe(d.getTransactionId());
                String status    = "PAID";
                log.info("[WEBHOOK] Transaction.Paid paymentId={}, txId={}", paymentId, txId);
                processingSvc.process("Transaction.Paid", paymentId, txId, status, rawBody == null ? "" : rawBody);

            } else if (webhook instanceof WebhookTransactionFailed failed) {
                var d = failed.getData(); // WebhookTransactionDataFailed
                String paymentId = safe(d.getPaymentId());
                String txId      = safe(d.getTransactionId());
                String status    = "FAILED";
                log.info("[WEBHOOK] Transaction.Failed paymentId={}, txId={}", paymentId, txId);
                processingSvc.process("Transaction.Failed", paymentId, txId, status, rawBody == null ? "" : rawBody);

            } else if (webhook instanceof WebhookTransactionPayPending pending) {
                var d = pending.getData(); // WebhookTransactionDataPayPending
                String paymentId = safe(d.getPaymentId());
                String txId      = safe(d.getTransactionId());
                String status    = "PENDING";
                log.info("[WEBHOOK] Transaction.PayPending paymentId={}, txId={}", paymentId, txId);
                processingSvc.process("Transaction.PayPending", paymentId, txId, status, rawBody == null ? "" : rawBody);

            } else if (webhook instanceof WebhookTransactionReady ready) {
                var d = ready.getData(); // WebhookTransactionDataReady
                String paymentId = safe(d.getPaymentId());
                String txId      = safe(d.getTransactionId());
                String status    = "READY";
                log.info("[WEBHOOK] Transaction.Ready paymentId={}, txId={}", paymentId, txId);
                processingSvc.process("Transaction.Ready", paymentId, txId, status, rawBody == null ? "" : rawBody);

            } else if (webhook instanceof WebhookTransactionCancelledCancelled cancelled) {
                var d = cancelled.getData(); // ...Cancelled Data
                String paymentId = safe(d.getPaymentId());
                String txId      = safe(d.getTransactionId());
                String status    = "CANCELLED";
                log.info("[WEBHOOK] Transaction.Cancelled paymentId={}, txId={}", paymentId, txId);
                processingSvc.process("Transaction.Cancelled", paymentId, txId, status, rawBody == null ? "" : rawBody);

            } else {
                // 인식 못한 타입: 스킵(하지만 200 반환)
                log.info("[WEBHOOK] Unrecognized/Unhandled webhook type: {}", webhook.getClass().getSimpleName());
            }

        } catch (Exception e) {
            // 비즈니스 예외는 200으로 마감(포트원 재시도 폭주 방지). 내부에서 재처리.
            log.error("[WEBHOOK] dispatch error", e);
        }

        return ResponseEntity.ok().build(); // 검증 성공 시 항상 2xx
    }

    private static String safe(String s) { return s == null ? "" : s; }
}
