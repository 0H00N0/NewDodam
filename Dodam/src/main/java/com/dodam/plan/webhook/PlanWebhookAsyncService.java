// src/main/java/com/dodam/plan/webhook/WebhookAsyncService.java
package com.dodam.plan.webhook;

import com.dodam.plan.Entity.PlanInvoiceEntity;
import com.dodam.plan.repository.PlanInvoiceRepository;
import com.dodam.plan.service.PlanBillingService;
import com.dodam.plan.service.PlanPaymentGatewayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanWebhookAsyncService {

    private final PlanInvoiceRepository invoiceRepo;
    private final PlanBillingService billingSvc;
    private final PlanPaymentGatewayService pgSvc;

    @Async("webhookExecutor")
    public void handleWebhook(String paymentId, String statusRaw, String receiptUrl, String rawJson) {
        try {
            if (!StringUtils.hasText(paymentId)) {
                log.warn("[WEBHOOK] skip: empty paymentId");
                return;
            }
            Optional<PlanInvoiceEntity> opt = invoiceRepo.findByPiUid(paymentId);
            if (opt.isEmpty()) {
                // 아직 어떤 인보이스에도 바인딩 안 된 경우 → 이후 confirm/lookup/잡에서 수렴
                log.warn("[WEBHOOK] invoice not found by paymentId={}, keep 200", paymentId);
                return;
            }
            Long invoiceId = opt.get().getPiId();

            String st = (statusRaw == null ? "" : statusRaw.trim().toUpperCase(Locale.ROOT));
            // 1) 웹훅 본문이 이미 확정 상태면 그걸 우선
            if (isPaid(st)) {
                billingSvc.recordAttempt(invoiceId, true, null, paymentId, receiptUrl, rawJson);
                return;
            }
            if (isFailed(st)) {
                billingSvc.recordAttempt(invoiceId, false, "LOOKUP:" + st, paymentId, receiptUrl, rawJson);
                return;
            }

            // 2) 애매하면 짧은 재조회(총 ~3초 내외)
            for (int i = 0; i < 10; i++) {
                var look = pgSvc.safeLookup(paymentId);
                String ls = String.valueOf(look.status()).toUpperCase(Locale.ROOT);
                if (isPaid(ls)) {
                    billingSvc.recordAttempt(invoiceId, true, null, paymentId, receiptUrl, look.rawJson());
                    return;
                }
                if (isFailed(ls)) {
                    billingSvc.recordAttempt(invoiceId, false, "LOOKUP:" + ls, paymentId, receiptUrl, look.rawJson());
                    return;
                }
                Thread.sleep(300);
            }
            // 3) 그래도 모호하면 PENDING 유지(후속 웹훅/잡에서 수렴)
            billingSvc.recordAttempt(invoiceId, false, "LOOKUP:PENDING", paymentId, receiptUrl, rawJson);
        } catch (Exception e) {
            log.error("[WEBHOOK] async handle error: {}", e.toString(), e);
        }
    }

    private boolean isPaid(String s) {
        if (s == null) return false;
        s = s.trim().toUpperCase(Locale.ROOT);
        return s.equals("PAID") || s.equals("SUCCEEDED") || s.equals("SUCCESS") || s.equals("PARTIAL_PAID");
    }
    private boolean isFailed(String s) {
        if (s == null) return false;
        s = s.trim().toUpperCase(Locale.ROOT);
        return s.equals("FAILED") || s.equals("CANCELED") || s.equals("CANCELLED");
    }
}
