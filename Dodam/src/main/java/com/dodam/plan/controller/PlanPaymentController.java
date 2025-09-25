package com.dodam.plan.controller;

import com.dodam.member.entity.MemberEntity;
import com.dodam.plan.enums.PlanEnums.PiStatus;
import com.dodam.plan.repository.PlanInvoiceRepository;
import com.dodam.plan.repository.PlanPaymentRepository;
import com.dodam.plan.service.PlanBillingService;
import com.dodam.plan.service.PlanPaymentGatewayService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.dodam.plan.Entity.PlanInvoiceEntity;  // <-- 패키지 경로의 'Entity' 대소문자 주의
import com.dodam.plan.enums.PlanEnums;

import java.util.LinkedHashMap;
import java.util.Map;
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
    private final PlanPaymentGatewayService pgSvc;   // 외부 결제 게이트웨이 호출만
    private final PlanBillingService billingSvc;     // DB 기록/상태 확정/카드메타

    @Value("${payments.confirm.immediate.enabled:false}")
    private boolean confirmImmediate;

    private final ExecutorService paymentExecutor =
            new ThreadPoolExecutor(
                    1, 4, 60, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(100),
                    new CustomizableThreadFactory("payment-async-")
            );

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

        var inv = invoiceRepo.findById(invoiceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "INVOICE_NOT_FOUND"));

        // 이미 결제된 인보이스는 즉시 OK
        if (inv.getPiStat() == PiStatus.PAID) {
            return ResponseEntity.ok(Map.of("result","OK","status","PAID","invoiceId",invoiceId));
        }

        var pm = inv.getPlanMember();
        if (pm == null || pm.getMember() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MEMBER_NOT_BOUND_TO_INVOICE");
        }
        MemberEntity member = pm.getMember();
        final String mid = member.getMid();
        final String customerId = resolveCustomerId(member);
        final long amount = toLongAmount(inv.getPiAmount());

        var profile = paymentRepo.findDefaultByMember(mid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "NO_DEFAULT_CARD"));

        final String billingKey = profile.getPayKey();
        if (!StringUtils.hasText(billingKey)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "BILLING_KEY_NOT_FOUND");
        }

        // inv{invoiceId}-ts{epochMillis}
        final String paymentId = "inv" + invoiceId + "-ts" + System.currentTimeMillis();

        if (confirmImmediate) {
            // 동기 승인(개발용)
            var res = pgSvc.payByBillingKey(paymentId, billingKey, amount, customerId);

            // ✅ 항상 현재 인보이스로 기록
            Long targetInvoiceId = inv.getPiId();

            billingSvc.recordAttempt(targetInvoiceId,
                    res.success(), res.success()?null:res.failReason(),
                    res.paymentId(), res.receiptUrl(), res.rawJson());

            if (res.success()) {
                // 인보이스 상태는 billingSvc 내부에서 확정된다고 가정 (recordAttempt 연계)
                return ResponseEntity.ok(Map.of(
                        "result","OK","status","PAID",
                        "paymentId",res.paymentId(),
                        "invoiceId",targetInvoiceId));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                        "result","FAIL","reason",res.failReason(),
                        "paymentId",res.paymentId(),
                        "invoiceId",targetInvoiceId));
            }
        } else {
            // ✅ 비동기 승인(권장): 즉시 202 반환 → 프론트는 /payments/{paymentId} 폴링
            paymentExecutor.submit(() -> {
                try {
                    var r = pgSvc.payByBillingKey(paymentId, billingKey, amount, customerId);

                    Long targetInvoiceId = inv.getPiId();

                    if (!r.success() && "ACCEPTED".equalsIgnoreCase(r.failReason())) {
                        // 게이트웨이 접수됨
                        billingSvc.recordAttempt(targetInvoiceId, false, "ACCEPTED", r.paymentId(), r.receiptUrl(), r.rawJson());
                    } else {
                        billingSvc.recordAttempt(targetInvoiceId, r.success(), r.success()?null:r.failReason(),
                                r.paymentId(), r.receiptUrl(), r.rawJson());
                    }

                    boolean paid = r.success();
                    if (!paid) {
                        // 🔁 짧은 지연 조회(2초 간격, 최대 8초)
                        for (int i = 0; i < 4; i++) {
                            try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                            var lookup = pgSvc.safeLookup(paymentId);
                            String st = String.valueOf(lookup.status()).toUpperCase();
                            if ("PAID".equals(st) || "SUCCEEDED".equals(st) || "SUCCESS".equals(st)) {
                                billingSvc.recordAttempt(targetInvoiceId, true, null, paymentId, null, lookup.rawJson());
                                paid = true; break;
                            }
                            if ("FAILED".equals(st) || "CANCELED".equals(st)) {
                                billingSvc.recordAttempt(targetInvoiceId, false, "LOOKUP:"+st, paymentId, null, lookup.rawJson());
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("[payments/confirm-async] paymentId={}, error: {}", paymentId, e.toString(), e);
                }
            });

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("result", "ACCEPTED");
            resp.put("paymentId", paymentId);
            resp.put("invoiceId", invoiceId);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(resp);
        }
    }

    /**
     * (직접 호출형) 결제키 결제 – 필요 시 유지
     */
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
            // 실패 응답 그대로 노출 (디버깅 편의)
            return ResponseEntity.status(502).body(Map.of(
                    "success", false,
                    "paymentId", pay.paymentId(),
                    "status", pay.status(),
                    "fail", pay.failReason(),
                    "payRaw", pay.rawJson()
            ));
        }

        // 간단 보정 폴링
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

    /**
     * (유지) 외부 상태 원문 확인용
     */
    @GetMapping("/{paymentId}/status")
    public ResponseEntity<?> status(@PathVariable("paymentId") String paymentId) {
        var r = pgSvc.safeLookup(paymentId);
        return ResponseEntity.ok(Map.of(
            "paymentId", r.paymentId(),
            "status",    r.status(),
            "raw",       r.rawJson()
        ));
    }

    /**
     * ✅ 프론트 폴링 엔드포인트: "DB 인보이스 상태"를 최우선으로 판단한다.
     * - PAID / FAILED / CANCELED 이면 즉시 done=true 로 반환
     * - 그 외(PENDING 등)일 때만 게이트웨이 조회 값을 보조로 사용
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<?> getPaymentStatus(@PathVariable("paymentId") String paymentId) {
        Long invoiceId = parseInvoiceId(paymentId);

        // (A) 인보이스 조회 (Optional<PlanInvoiceEntity>)
        var invOpt = (invoiceId != null)
                ? invoiceRepo.findById(invoiceId)
                : java.util.Optional.<PlanInvoiceEntity>empty();

        // 공통 캐시 방지 헤더
        HttpHeaders nocache = new HttpHeaders();
        nocache.add("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        nocache.add("Pragma", "no-cache");

        if (invOpt.isPresent()) {
            PlanInvoiceEntity inv = invOpt.get();

            // 1) DB 상태로 우선 판단
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
                                "paidAt",    inv.getPiPaid() // 있으면 전달
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

            // 2) 아직 DB가 PENDING이면, 게이트웨이 상태 보조
            var r = pgSvc.safeLookup(paymentId);
            String s = String.valueOf(r.status()).toUpperCase();

            boolean gwDone = switch (s) {
                case "PAID", "SUCCEEDED", "SUCCESS", "FAILED", "CANCELED", "CANCELLED", "NOT_FOUND" -> true;
                default -> false; // PENDING / UNKNOWN 등
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

        // 3) 인보이스 못 찾으면 게이트웨이만 의존
        var r = pgSvc.safeLookup(paymentId);
        String s = String.valueOf(r.status()).toUpperCase();
        boolean gwDone = switch (s) {
            case "PAID", "SUCCEEDED", "SUCCESS", "FAILED", "CANCELED", "CANCELLED", "NOT_FOUND" -> true;
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

    /** "inv{digits}-ts{digits}" → digits 추출 */
    private static final Pattern INV_PAT = Pattern.compile("^inv(\\d+)-ts\\d+$");
    private static Long parseInvoiceId(String paymentId) {
        if (!StringUtils.hasText(paymentId)) return null;
        Matcher m = INV_PAT.matcher(paymentId);
        if (m.matches()) {
            try { return Long.parseLong(m.group(1)); }
            catch (Exception ignore) { return null; }
        }
        return null;
    }

    /** 소수 금액 → 정수 KRW (반올림) */
    private static long toLongAmount(java.math.BigDecimal b) {
        if (b == null) return 0L;
        return Math.max(b.setScale(0, java.math.RoundingMode.HALF_UP).longValue(), 0L);
    }

    /** 멤버에서 customerId 유추(선택 필드) */
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
