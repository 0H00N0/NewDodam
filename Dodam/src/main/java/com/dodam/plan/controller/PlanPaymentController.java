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
import com.dodam.plan.service.PlanPortoneClientService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpSession;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.security.core.Authentication;
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
    
    private final PlanPortoneClientService portoneClient;

    @Value("${payments.confirm.immediate.enabled:false}")
    private boolean confirmImmediate;

    private final ExecutorService paymentExecutor =
            new ThreadPoolExecutor(
                    1, 4, 60, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(100),
                    new CustomizableThreadFactory("payment-async-")
            );

    /** 인보이스 기반 결제 확정 (UUID 대신 orderId 사용) */
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

        // ✅ 서버가 만드는 유일한 orderId: inv{invoiceId}-ts{epochMillis}
        final String orderId = "inv" + invoiceId + "-ts" + System.currentTimeMillis();

        // 비동기 confirm → 결과는 웹훅/조회로 최종 반영
        paymentExecutor.submit(() -> {
            try {
                JsonNode confirmRes = pgSvc.confirmBilling(orderId, billingKey, amount, "KRW", "Dodam Subscription", customerId);

                // 1) 우선 orderId 기준으로 기록 (ACCEPTED/PENDING 단계)
                String rawJson = confirmRes != null ? confirmRes.toString() : null;
                billingSvc.recordAttempt(inv.getPiId(), false, "ACCEPTED",
                        /* respUid */ orderId, /* receiptUrl */ null, rawJson);

                // 2) 정확 매칭 성공 시 paymentId로 보강 기록
                pgSvc.findPaymentByExactOrderId(orderId).ifPresent(hit -> {
                    String pid = hit.path("id").asText(null);
                    billingSvc.recordAttempt(inv.getPiId(), false, "ACCEPTED",
                            /* respUid */ pid, /* receiptUrl */ null, hit.toString());
                    log.info("[payments/confirm-async] orderId={} -> paymentId={}", orderId, pid);
                });

            } catch (Exception e) {
                log.error("[payments/confirm-async] orderId={}, error: {}", orderId, e.toString(), e);
            }
        });

        // ✅ 응답 작성: 즉시 한 번만 orderId로 paymentId 정확 일치 조회 시도 (실패해도 무시)
        String providerPaymentId = null;
        try {
            Optional<JsonNode> hit = pgSvc.findPaymentByExactOrderId(orderId);
            if (hit.isPresent()) {
                providerPaymentId = hit.get().path("id").asText(null);
            }
        } catch (Exception ignore) {}

        Map<String,Object> res = new LinkedHashMap<>();
        res.put("result","ACCEPTED");
        res.put("orderId",orderId);
        res.put("invoiceId",invoiceId);
        if (providerPaymentId != null) res.put("paymentId", providerPaymentId); // 있으면 얹기

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(res);
    }

    /** 직접 billingKey 결제(디버그용) — 기존 유지 */
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

    /** 간단 상태 조회 (paymentId 또는 orderId 모두 허용) */
    @GetMapping("/{id}")
    public ResponseEntity<?> getPaymentStatus(@PathVariable("id") String id) {
        HttpHeaders nocache = new HttpHeaders();
        nocache.add("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        nocache.add("Pragma", "no-cache");

        String paymentId = null;

        if (isPaymentId(id)) {
            paymentId = id;

        } else if (isOrderId(id)) {
            try {
                Optional<JsonNode> hit = pgSvc.findPaymentByExactOrderId(id);
                if (hit.isPresent()) {
                    paymentId = hit.get().path("id").asText(null);
                } else {
                    // 정확 매칭 아직 없음 → PENDING or TIMEOUT (orderId 생성 후 15초 경과 시 종료)
                    Long invoiceId = parseInvoiceIdFromOrderId(id);
                    Long ts = parseTsFromOrderId(id);
                    boolean timeout = (ts != null) && (System.currentTimeMillis() - ts > 15_000);

                    Map<String,Object> resp = new LinkedHashMap<>();
                    resp.put("paymentId", null);
                    if (invoiceId != null) resp.put("invoiceId", invoiceId);
                    resp.put("status", timeout ? "TIMEOUT" : "PENDING");
                    resp.put("done", timeout);
                    return ResponseEntity.ok().headers(nocache).body(resp);
                }
            } catch (Exception e) {
                log.warn("[payments/status] findPaymentByExactOrderId failed: {}", e.toString());
                Long invoiceId = parseInvoiceIdFromOrderId(id);
                Map<String,Object> resp = new LinkedHashMap<>();
                resp.put("paymentId", null);
                if (invoiceId != null) resp.put("invoiceId", invoiceId);
                resp.put("status", "PENDING");
                resp.put("done", false);
                return ResponseEntity.ok().headers(nocache).body(resp);
            }
        }

        // paymentId를 확보 못했으면 여기서 종료 (404 스팸/무한루프 방지)
        if (!StringUtils.hasText(paymentId)) {
            Long invoiceId = isOrderId(id) ? parseInvoiceIdFromOrderId(id) : parseInvoiceId(id);
            Map<String,Object> resp = new LinkedHashMap<>();
            resp.put("paymentId", null);
            if (invoiceId != null) resp.put("invoiceId", invoiceId);
            resp.put("status", "PENDING");
            resp.put("done", false);
            return ResponseEntity.ok().headers(nocache).body(resp);
        }

        // paymentId 확보 시 DB 먼저 확인
        Long invoiceId = parseInvoiceId(paymentId);
        Optional<PlanInvoiceEntity> invOpt = (invoiceId != null)
                ? invoiceRepo.findById(invoiceId)
                : Optional.empty();

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
        }

        // DB로 확정되지 않은 경우 게이트웨이 조회(단, 무조건 paymentId 사용)
        var r = pgSvc.safeLookup(paymentId);
        String s = String.valueOf(r.status()).toUpperCase();

        // ✅ NOT_FOUND도 종료로 간주하여 폴링 중단
        boolean gwDone = switch (s) {
            case "PAID", "SUCCEEDED", "SUCCESS", "PARTIAL_PAID", "FAILED", "CANCELED", "CANCELLED", "NOT_FOUND" -> true;
            case "PENDING", "ACCEPTED", "READY" -> false;
            default -> false;
        };

        Map<String,Object> resp = new LinkedHashMap<>();
        resp.put("paymentId", StringUtils.hasText(r.paymentId()) ? r.paymentId() : paymentId);
        if (invoiceId != null) resp.put("invoiceId", invoiceId);
        resp.put("status", s);
        resp.put("done", gwDone);
        resp.put("raw", r.rawJson());

        return ResponseEntity.ok().headers(nocache).body(resp);
    }
    
    /** ✅ 결제 취소(부분/전액) */
    @PostMapping("/{paymentId}/cancel")
    public ResponseEntity<Map<String,Object>> cancelPayment(
            @PathVariable("paymentId") String paymentId,
            @RequestBody CancelReq req,
            Authentication auth
    ) {
        final String mid = (auth != null ? auth.getName() : null);
        if (mid == null || mid.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "LOGIN_REQUIRED"));
        }

        var r = portoneClient.cancelPayment(
                paymentId,
                req.getAmount(), req.getTaxFreeAmount(), req.getVatAmount(),
                (req.getReason() != null ? req.getReason() : "user_requested")
        );

        // 우리 DB 인보이스도 정리 (있는 경우)
        invoiceRepo.findByPiUid(paymentId).ifPresent(inv -> {
            // 부분취소/전액취소를 세밀히 반영하려면 별도 금액/상태 컬럼 설계를 보강하세요.
            inv.setPiStat(com.dodam.plan.enums.PlanEnums.PiStatus.CANCELED);
            invoiceRepo.save(inv);
        });

        return ResponseEntity.ok(Map.of(
                "status", r.status(),
                "raw", r.raw()
        ));
    }

    @Data
    public static class CancelReq {
        private Long amount;        // null이면 전액
        private Long taxFreeAmount; // 선택
        private Long vatAmount;     // 선택
        private String reason;      // 필수(문서상)
    }

    // ====== ID 판별/파싱 유틸 ======

    // 기존 paymentId 패턴: inv{digits}-u{uuid...}
    private static final Pattern PAYMENT_ID_PAT = Pattern.compile("^inv(\\d+)-u[0-9a-fA-F-]{8,}$");
    // 새 orderId 패턴: inv{digits}-ts{epochMillis}
    private static final Pattern ORDER_ID_PAT   = Pattern.compile("^inv\\d+-ts\\d+$");

    private static boolean isPaymentId(String s) { return s != null && PAYMENT_ID_PAT.matcher(s).matches(); }
    private static boolean isOrderId(String s) { return s != null && ORDER_ID_PAT.matcher(s).matches(); }

    private static Long parseInvoiceId(String paymentId) {
        if (!StringUtils.hasText(paymentId)) return null;
        Matcher m = PAYMENT_ID_PAT.matcher(paymentId);
        if (m.matches()) {
            try { return Long.parseLong(m.group(1)); }
            catch (Exception ignore) { return null; }
        }
        return null;
    }

    private static Long parseInvoiceIdFromOrderId(String orderId) {
        if (!isOrderId(orderId)) return null;
        try {
            String num = orderId.replaceFirst("^inv","").split("-ts")[0].replaceAll("[^0-9]","");
            return Long.parseLong(num);
        } catch (Exception e) { return null; }
    }

    /** orderId(inv{invoice}-ts{millis})에서 millis 추출 */
    private static Long parseTsFromOrderId(String orderId) {
        if (!isOrderId(orderId)) return null;
        try {
            String ts = orderId.substring(orderId.indexOf("-ts") + 3);
            return Long.parseLong(ts);
        } catch (Exception e) { return null; }
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
