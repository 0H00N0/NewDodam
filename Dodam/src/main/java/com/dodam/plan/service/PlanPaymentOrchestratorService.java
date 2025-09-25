package com.dodam.plan.service;

import com.dodam.plan.Entity.PlanInvoiceEntity;
import com.dodam.plan.Entity.PlanPaymentEntity;
import com.dodam.plan.repository.PlanInvoiceRepository;
import com.dodam.plan.repository.PlanPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlanPaymentOrchestratorService {

    private final PlanPaymentGatewayService pgSvc;
    private final PlanBillingService billingSvc;          // recordAttempt(...) 제공
    private final PlanInvoiceRepository invoiceRepo;
    private final PlanPaymentRepository paymentRepo;

    /** 배치/재시도: 인보이스 ID만으로 결제 시도 */
    @Transactional
    public void tryPayInvoice(Long piId) {
        var inv = invoiceRepo.findById(piId)
                .orElseThrow(() -> new IllegalArgumentException("invoice not found: " + piId));

        String mid = extractMidFromInvoice(inv);

        PlanPaymentEntity payment = paymentRepo.findTopByMidOrderByPayIdDesc(mid)
                .orElseThrow(() -> new IllegalStateException("no payment profile for mid=" + mid));

        confirmInvoice(inv, payment);
    }

    /** 인보이스 단건 승인(or 재시도) */
    @Transactional
    public void confirmInvoice(PlanInvoiceEntity inv, PlanPaymentEntity payment) {
        log.info("[confirmInvoice] invId={}, piUid={}, amount={}, mid={}",
                inv.getPiId(), inv.getPiUid(), inv.getPiAmount(), payment.getMid());

        String customerId = payment.getPayCustomer();
        if (customerId == null || customerId.isBlank()) {
            customerId = payment.getMid();
        }

        var res = pgSvc.payByBillingKey(
                inv.getPiUid(),                    // paymentId
                payment.getPayKey(),               // billingKey
                inv.getPiAmount().longValue(),     // amount
                customerId
        );

        String uid      = res.paymentId();
        String reason   = res.failReason();
        String receipt  = res.receiptUrl();
        boolean success = res.success();
        String rawJson  = (res.rawJson() != null) ? res.rawJson() : "{}";

        billingSvc.recordAttempt(
                inv.getPiId(),
                success,
                reason,
                uid,
                receipt,
                rawJson
        );

        log.info("[confirmInvoice] result: success={}, uid={}, reason={}", success, uid, reason);
    }

    private String extractMidFromInvoice(PlanInvoiceEntity inv) {
        if (inv.getPlanMember() == null) {
            throw new IllegalStateException("invoice has no PlanMember linked");
        }
        return inv.getPlanMember().getMember().getMid();
    }
}
