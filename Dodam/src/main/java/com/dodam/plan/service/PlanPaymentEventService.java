package com.dodam.plan.service;

import com.dodam.plan.Entity.PlanInvoiceEntity;
import com.dodam.plan.enums.PlanEnums.PiStatus;
import com.dodam.plan.repository.PlanInvoiceRepository;
import com.dodam.plan.repository.PlanPaymentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanPaymentEventService {

    private final PlanInvoiceRepository invoiceRepo;
    private final PlanPaymentRepository paymentRepo;
    private final PlanBillingService billingSvc;

    @Transactional
    public void onPaid(JsonNode root) {
        JsonNode payload = root == null ? null : root.path("payload");

        String paymentId = pick(payload,
                "paymentId","id","uid","merchantUid","txId","transactionUid","transaction_uid","tx_id");
        if (!StringUtils.hasText(paymentId)) {
            log.warn("[paid] missing paymentId. payload={}", safe(payload));
            return;
        }

        String brand  = pick(payload, "card.company","card.brand","card.issuer","methodDetail.brand");
        String bin    = pick(payload, "card.bin");
        String last4  = firstNonBlank(
                pick(payload,"card.lastFourDigits","card.last4"),
                tail4(pick(payload,"card.number","card.cardNumber"))
        );
        String pgName = firstNonBlank(pick(payload,"pgProvider","gateway","pg","provider"));
        String payKey = firstNonBlank(pick(payload,"billingKey","billing_key","payKey"));

        String receiptUrl = firstNonBlank(
                pick(payload,"receiptUrl","card.receiptUrl","cashReceipt.url"));

        Optional<PlanInvoiceEntity> optInv = invoiceRepo.findByPiUid(paymentId);
        if (optInv.isEmpty()) {
            optInv = parseInvoiceIdFrom(paymentId).flatMap(invoiceRepo::findById);
        }

        if (optInv.isPresent()) {
            PlanInvoiceEntity inv = optInv.get();

            if (PiStatus.PAID.equals(inv.getPiStat())) {
                if (!StringUtils.hasText(inv.getPiUid())) {
                    invoiceRepo.markPaidAndSetUidIfEmpty(inv.getPiId(), paymentId, LocalDateTime.now());
                }
                billingSvc.recordAttempt(inv.getPiId(), true, null, paymentId, receiptUrl, safe(root));
            } else {
                invoiceRepo.markPaidAndSetUidIfEmpty(inv.getPiId(), paymentId, LocalDateTime.now());
                billingSvc.recordAttempt(inv.getPiId(), true, null, paymentId, receiptUrl, safe(root));
            }
        } else {
            log.warn("[paid] invoice not found for paymentId={}", paymentId);
        }

        // 카드 메타: billingKey 기준 업데이트
        if (StringUtils.hasText(payKey)) {
            boolean updated = persistCardMetaByKey(payKey, bin, brand, last4, pgName);
            log.debug("[paid] card meta updated by key? {}", updated);
        }
    }

    @Transactional
    public void onPartiallyPaid(JsonNode root) {
        log.info("[partially_paid] payload={}", safe(root));
    }

    @Transactional
    public void onCancelled(JsonNode root) {
        log.info("[cancelled] payload={}", safe(root));
    }

    /* ===== helpers ===== */

    private static String pick(JsonNode root, String... paths) {
        if (root == null) return null;
        for (String p : paths) {
            String v = path(root, p);
            if (StringUtils.hasText(v)) return v;
        }
        return null;
    }

    private static String path(JsonNode root, String dotPath) {
        if (root == null) return null;
        JsonNode cur = root;
        for (String seg : dotPath.split("\\.")) {
            cur = cur.path(seg);
            if (cur.isMissingNode() || cur.isNull()) return null;
        }
        return cur.isValueNode() ? cur.asText(null) : null;
    }

    private static String tail4(String masked) {
        if (!StringUtils.hasText(masked)) return null;
        String s = masked.replaceAll("[^0-9]", "");
        if (s.length() < 4) return null;
        return s.substring(s.length() - 4);
    }

    private static String firstNonBlank(String... v){
        if (v == null) return null;
        for (String s : v) if (StringUtils.hasText(s)) return s;
        return null;
    }

    private static Optional<Long> parseInvoiceIdFrom(String anyId) {
        try {
            String onlyNum = anyId == null ? "" : anyId
                    .replaceFirst("^inv","")
                    .split("-")[0]
                    .replaceAll("[^0-9]","");
            if (onlyNum.isEmpty()) return Optional.empty();
            return Optional.of(Long.parseLong(onlyNum));
        } catch (Exception ignore) {
            return Optional.empty();
        }
    }

    private boolean persistCardMetaByKey(String payKey, String bin, String brand, String last4, String pg) {
        if (!StringUtils.hasText(payKey)) return false;
        return paymentRepo.findByPayKey(payKey).map(p -> {
            boolean changed = false;
            String dLast4 = sanitizeLast4(last4);
            if (StringUtils.hasText(bin)   && !bin.equals(p.getPayBin()))     { p.setPayBin(bin);       changed = true; }
            if (StringUtils.hasText(brand) && !brand.equals(p.getPayBrand())) { p.setPayBrand(brand);   changed = true; }
            if (StringUtils.hasText(dLast4)&& !dLast4.equals(p.getPayLast4())){ p.setPayLast4(dLast4);  changed = true; }
            if (StringUtils.hasText(pg)    && !pg.equals(p.getPayPg()))       { p.setPayPg(pg);         changed = true; }
            if (changed) paymentRepo.save(p);
            return changed;
        }).orElse(false);
    }
    private static String sanitizeLast4(String v) {
        if (!StringUtils.hasText(v)) return null;
        String digits = v.replaceAll("\\D", "");
        if (digits.length() >= 4) return digits.substring(digits.length() - 4);
        return null;
    }

    private static String safe(JsonNode n){ try { return n == null ? null : n.toString(); } catch (Exception e){ return null; } }
    @SuppressWarnings("unused")
    private static String up(String s){ return s==null? null : s.trim().toUpperCase(Locale.ROOT); }
}
