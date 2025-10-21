package com.dodam.plan.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.dodam.plan.repository.PlanAttemptRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlanAttemptQueryService {
    private final PlanAttemptRepository attemptRepo;

    public Optional<String> getReceiptUrl(Long invoiceId, String paymentUid) {
        if (invoiceId != null) {
            var byInvoice = attemptRepo.findLatestReceiptUrlByInvoiceId(invoiceId);
            if (byInvoice.isPresent()) return byInvoice;
        }
        if (StringUtils.hasText(paymentUid)) {
            var byPayment = attemptRepo.findLatestReceiptUrlByPaymentUid(paymentUid);
            if (byPayment.isPresent()) return byPayment;
        }
        return Optional.empty();
    }
}
