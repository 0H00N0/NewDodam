// src/main/java/com/dodam/plan/webhook/PlanWebhookProcessingService.java
package com.dodam.plan.webhook;

import com.dodam.plan.dto.PlanLookupResult;
import com.dodam.plan.service.PlanPaymentGatewayService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanWebhookProcessingService {

    private final PlanPaymentGatewayService gateway;
    private final ObjectMapper mapper;

    /** webhookExecutor 풀에서 비동기로 실행 */
    @Async("webhookExecutor")
    public void process(String type, String paymentId, String transactionId, String status, String rawBody) {
        try {
            log.info("[WebhookJob] type={}, paymentId={}, txId={}, status={}", type, paymentId, transactionId, status);

            PlanLookupResult look = gateway.safeLookup(paymentId);
            String payload = (look == null) ? "null" : mapper.writeValueAsString(look);
            log.info("[WebhookJob] lookup={}", payload);

            // TODO: 도메인 업데이트 로직 (예: billingSvc.applyWebhook(paymentId, status, look))

        } catch (Exception e) {
            log.error("[WebhookJob] processing error", e);
        }
    }
}
