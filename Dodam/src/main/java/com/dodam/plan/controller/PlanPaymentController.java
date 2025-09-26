package com.dodam.plan.controller;

import com.dodam.member.entity.MemberEntity;
import com.dodam.plan.Entity.PlanInvoiceEntity;
import com.dodam.plan.Entity.PlanMember;
import com.dodam.plan.Entity.PlanPaymentEntity;
import com.dodam.plan.enums.PlanEnums.PiStatus;
import com.dodam.plan.repository.PlanInvoiceRepository;
import com.dodam.plan.repository.PlanPaymentRepository;
import com.dodam.plan.service.PlanBillingService;
import com.dodam.plan.service.PlanPaymentGatewayService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/payments")
public class PlanPaymentController {

    private final PlanInvoiceRepository invoiceRepo;
    private final PlanPaymentRepository paymentRepo;
    private final PlanPaymentGatewayService pgSvc;
    private final PlanBillingService billingSvc;

    @Value("${payments.confirm.immediate.enabled:false}")
    private boolean confirmImmediate;

    private final ExecutorService paymentExecutor =
            new ThreadPoolExecutor(
                    1, 4, 60, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(100),
                    new CustomizableThreadFactory("payment-async-")
            );

    /** 인보이스 기반 결제 확정 */
    @PostMapping(value = "/confirm", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> confirm(@RequestBody Map<String, Object> body, HttpSession session) {
        String sid = (String) session.getAttribute("sid");
        if (!StringUtils.hasText(sid)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("result","FAIL","reason","LOGIN_REQUIRED"));
        }

        long invoiceId;
        try {
            invoiceId = Long.parseLong(String.valueOf(body.get("invoiceId")));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("result","FAIL","reason","INVALID_INVOICE_ID"));
        }

        PlanInvoiceEntity inv = invoiceRepo.findById(invoiceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "INVOICE_NOT_FOUND"));

        if (inv.getPiStat() == PiStatus.PAID) {
            return ResponseEntity.ok(Map.of("result","OK","status","PAID","invoiceId",invoiceId));
        }

        PlanMember pm = inv.getPlanMember();
        if (pm == null || pm.getPayment() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PAYMENT_NOT_BOUND_TO_INVOICE");
        }

        final PlanPaymentEntity payment = pm.getPayment();
        final String billingKey = payment.getPayKey();
        if (!StringUtils.hasText(billingKey)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "BILLING_KEY_NOT_FOUND");
        }

        final String customerId = resolveCustomerId(pm.getMember());
        final long amount = toLongAmount(inv.getPiAmount());

        // 고유 paymentId 생성: inv{invoiceId}-u{uuid}
        final String paymentId = "inv" + invoiceId + "-u" + UUID.randomUUID();

        // 비동기 confirm → 결과는 웹훅/추후조회로 반영
        paymentExecutor.submit(() -> {
            try {
                var r = pgSvc.payByBillingKey(paymentId, billingKey, amount, customerId);
                // 웹훅이 최종 진실 → 여기서는 "대기"로 기록
                billingSvc.recordAttempt(inv.getPiId(), false, "ACCEPTED", paymentId, null, r.rawJson());
            } catch (Exception e) {
                log.error("[payments/confirm-async] paymentId={}, error: {}", paymentId, e.toString(), e);
            }
        });

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                "result","ACCEPTED","paymentId",paymentId,"invoiceId",invoiceId));
    }

    /** 직접 billingKey 결제(디버그용) */
    @PostMapping(path = "/{paymentId}/billing-key", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> payByBillingKey(
            @PathVariable("paymentId") String paymentId,
            @RequestBody Map<String, Object> req) {

        String billingKey = String.valueOf(req.get("billingKey"));
        long amount       = Long.parseLong(String.valueOf(req.get("amount")));
        String currency   = String.valueOf(req.getOrDefault("currency", "KRW"));
        String orderName  = String.valueOf(req.getOrDefault("orderName", "Dodam Subscription"));
        String storeId    = String.valueOf(req.get("storeId"));
        String customerId = (String) req.getOrDefault("customerId", null);
        String channelKey = (String) req.getOrDefault("channelKey", null);

        var pay = pgSvc.payByBillingKey(paymentId, billingKey, amount, currency, orderName, storeId, customerId, channelKey);

        if (!pay.success()) {
            return ResponseEntity.status(502).body(Map.of(
                    "success", false,
                    "paymentId", pay.paymentId(),
                    "status", pay.status(),
                    "fail", pay.failReason(),
                    "payRaw", pay.rawJson()
            ));
        }

        var lookup = pgSvc.safeLookup(pay.paymentId());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "paymentId", pay.paymentId(),
                "status",    lookup != null ? lookup.status() : pay.status(),
                "receiptUrl", pay.receiptUrl(),
                "payRaw",     pay.rawJson(),
                "lookupRaw",  lookup != null ? lookup.rawJson() : null
        ));
    }

    /** 간단 상태 조회 (DB 우선, 그 외 게이트웨이 보조) */
    @GetMapping("/{paymentId}")
    public ResponseEntity<?> getPaymentStatus(@PathVariable("paymentId") String paymentId) {
        Long invoiceId = parseInvoiceId(paymentId);

        var invOpt = (invoiceId != null)
                ? invoiceRepo.findById(invoiceId)
                : Optional.<PlanInvoiceEntity>empty();

        HttpHeaders nocache = new HttpHeaders();
        nocache.add("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        nocache.add("Pragma", "no-cache");

        if (invOpt.isPresent()) {
            PlanInvoiceEntity inv = invOpt.get();

            boolean dbPaid   = (inv.getPiStat() == PiStatus.PAID) || (inv.getPiPaid() != null);
            boolean dbFailed = (inv.getPiStat() == PiStatus.FAILED);
            boolean dbCancel = (inv.getPiStat() == PiStatus.CANCELED);

            if (dbPaid) {
                return ResponseEntity.ok()
                        .headers(nocache)
                        .body(Map.of(
                                "paymentId", paymentId,
                                "invoiceId", inv.getPiId(),
                                "status",    "PAID",
                                "done",      true,
                                "paidAt",    inv.getPiPaid()
                        ));
            }
            if (dbFailed || dbCancel) {
                return ResponseEntity.ok()
                        .headers(nocache)
                        .body(Map.of(
                                "paymentId", paymentId,
                                "invoiceId", inv.getPiId(),
                                "status",    inv.getPiStat().name(),
                                "done",      true
                        ));
            }

            var r = pgSvc.safeLookup(paymentId);
            String s = String.valueOf(r.status()).toUpperCase();

            boolean gwDone = switch (s) {
                case "PAID", "SUCCEEDED", "SUCCESS", "PARTIAL_PAID", "FAILED", "CANCELED", "CANCELLED" -> true;
                case "NOT_FOUND", "PENDING", "ACCEPTED", "READY" -> false; // 생성 전/승인대기/준비 상태는 계속 폴링
                default -> false;
            };

            return ResponseEntity.ok()
                    .headers(nocache)
                    .body(Map.of(
                            "paymentId", paymentId,
                            "invoiceId", inv.getPiId(),
                            "status",    s,
                            "done",      gwDone,
                            "raw",       r.rawJson()
                    ));
        }

        var r = pgSvc.safeLookup(paymentId);
        String s = String.valueOf(r.status()).toUpperCase();

        boolean gwDone = switch (s) {
            case "PAID", "SUCCEEDED", "SUCCESS", "PARTIAL_PAID", "FAILED", "CANCELED", "CANCELLED" -> true;
            case "NOT_FOUND", "PENDING", "ACCEPTED", "READY" -> false; // 생성 전/승인대기/준비 상태는 계속 폴링
            default -> false;
        };

        return ResponseEntity.ok()
                .headers(nocache)
                .body(Map.of(
                        "paymentId", r.paymentId(),
                        "status",    s,
                        "done",      gwDone,
                        "raw",       r.rawJson()
                ));
    }

    // ✅ 새로운 ID 패턴: inv{digits}-u{uuid...}
    private static final Pattern INV_PAT = Pattern.compile("^inv(\\d+)-u[0-9a-fA-F-]{8,}$");

    private static Long parseInvoiceId(String paymentId) {
        if (!StringUtils.hasText(paymentId)) return null;
        Matcher m = INV_PAT.matcher(paymentId);
        if (m.matches()) {
            try { return Long.parseLong(m.group(1)); }
            catch (Exception ignore) { return null; }
        }
        return null;
    }

    private static long toLongAmount(java.math.BigDecimal b) {
        if (b == null) return 0L;
        return Math.max(b.setScale(0, java.math.RoundingMode.HALF_UP).longValue(), 0L);
    }

    private static String resolveCustomerId(MemberEntity member) {
        try {
            var m = member.getClass().getMethod("getCustomerId");
            Object v = m.invoke(member);
            return (v instanceof String s) ? s : null;
        } catch (Exception ignore) {
            return null;
        }
    }
}
