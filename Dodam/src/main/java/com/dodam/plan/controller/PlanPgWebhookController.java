// src/main/java/com/dodam/plan/controller/PlanPgWebhookController.java
package com.dodam.plan.controller;

import com.dodam.plan.config.PlanPortoneProperties;
import com.dodam.plan.webhook.PlanWebhookProcessingService;
import io.portone.sdk.server.webhook.Webhook;
import io.portone.sdk.server.webhook.WebhookTransactionFailed;
import io.portone.sdk.server.webhook.WebhookTransactionPaid;
import io.portone.sdk.server.webhook.WebhookTransactionPayPending;
import io.portone.sdk.server.webhook.WebhookTransactionReady;
import io.portone.sdk.server.webhook.WebhookVerifier;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/webhooks")
public class PlanPgWebhookController {

    private final PlanPortoneProperties props;                  // application.properties의 portone.webhookSecret 사용
    private final PlanWebhookProcessingService processingSvc;   // 네가 이미 가진 @Service (process(...))

    @PostMapping(value = "/pg", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> handlePortoneWebhook(
            HttpServletRequest req,
            @RequestBody(required = false) byte[] rawBytes   // ★ 바이트로 수신 (인코딩 보존)
    ) {
        // 0) 원문 바디 UTF-8 복원
        String raw = (rawBytes == null) ? "" : new String(rawBytes, StandardCharsets.UTF_8);

        // ── 수신 요약 로그 ─────────────────────────────────────────────
        String ct = nz(req.getContentType());
        String sha256 = sha256Hex(raw);
        Map<String, String> headersAll = collectHeaders(req);
        log.info("[WEBHOOK/IN] ct={} body.len={} sha256={} headers={}",
                ct, raw.length(), sha256, safeHeaderPreview(headersAll));

        // ── 필수 헤더 추출(대소문자 안전) ───────────────────────────────
        String id  = firstNonBlank(
                req.getHeader(WebhookVerifier.HEADER_ID),         // webhook-id
                req.getHeader("Webhook-Id"),
                req.getHeader("X-Webhook-Id")
        );
        String sig = firstNonBlank(
                req.getHeader(WebhookVerifier.HEADER_SIGNATURE),  // webhook-signature
                req.getHeader("Webhook-Signature"),
                req.getHeader("X-Webhook-Signature")
        );
        String ts  = firstNonBlank(
                req.getHeader(WebhookVerifier.HEADER_TIMESTAMP),  // webhook-timestamp
                req.getHeader("Webhook-Timestamp"),
                req.getHeader("X-Webhook-Timestamp")
        );

        if (isBlank(id) || isBlank(sig) || isBlank(ts)) {
            log.warn("[WEBHOOK/ERR] missing headers id={} sig.len={} ts={}",
                    id, sig == null ? null : sig.length(), ts);
            return ResponseEntity.badRequest().body("missing headers");
        }

        // ── SDK 서명 검증 ───────────────────────────────────────────────
        String secret = nz(props.getWebhookSecret()); // 반드시 whsec_... (API v2 secret과 다름)
        Webhook event;
        try {
            WebhookVerifier verifier = new WebhookVerifier(secret);
            event = verifier.verify(raw, id, sig, ts);
        } catch (Exception ex) {
            log.warn("[WEBHOOK/VERIFY] SDK failed: {}", ex.toString());

            // ── 수동 HMAC 검증(디버그) ─────────────────────────────────
            DebugVerifyResult dbg = debugVerifyManual(secret, id, ts, sig, raw);
            log.warn("[WEBHOOK/DEBUG] manual expectedBase64={} actual.v1={} id={} ts={} t={} skewOk={} secretBase64Ok={}",
                    dbg.expectedBase64Preview, dbg.v1Preview, id, ts, dbg.tVal, dbg.skewOk, dbg.secretBase64Ok);

            return ResponseEntity.badRequest().body("invalid signature");
        }

        // ── 이벤트 타입별 분기 → 기존 서비스 호출 ───────────────────────
        try {
            if (event instanceof WebhookTransactionPaid e) {
                var d = e.getData();
                String paymentId = nz(d.getPaymentId());
                String txId      = nz(d.getTransactionId());
                log.info("[WEBHOOK/OK] PAID paymentId={} txId={}", paymentId, txId);
                String pid = (paymentId == null || paymentId.isBlank()) ? null : paymentId;
                String tid = (txId == null || txId.isBlank()) ? null : txId;
                processingSvc.process("Transaction.Paid", pid, tid, "PAID", raw);
                
            } else if (event instanceof WebhookTransactionFailed e) {
                var d = e.getData();
                String paymentId = nz(d.getPaymentId());
                String txId      = nz(d.getTransactionId());
                log.info("[WEBHOOK/OK] FAILED paymentId={} txId={}", paymentId, txId);
                processingSvc.process("Transaction.Failed", paymentId, txId, "FAILED", raw);

            } else if (event instanceof WebhookTransactionPayPending e) {
                var d = e.getData();
                String paymentId = nz(d.getPaymentId());
                String txId      = nz(d.getTransactionId());
                log.info("[WEBHOOK/OK] PENDING paymentId={} txId={}", paymentId, txId);
                processingSvc.process("Transaction.PayPending", paymentId, txId, "PENDING", raw);

            } else if (event instanceof WebhookTransactionReady e) {
                var d = e.getData();
                String paymentId = nz(d.getPaymentId());
                String txId      = nz(d.getTransactionId());
                log.info("[WEBHOOK/OK] READY paymentId={} txId={}", paymentId, txId);
                processingSvc.process("Transaction.Ready", paymentId, txId, "READY", raw);

            } else {
                log.info("[WEBHOOK/OK] Unhandled type: {}", event.getClass().getSimpleName());
            }
        } catch (Exception ex) {
            // 비즈니스 에러가 나더라도 200으로 마감(재시도 폭주 방지). 내부에서 재처리.
            log.error("[WEBHOOK/DISPATCH] error", ex);
        }

        return ResponseEntity.ok("ok");
    }

    // ====================== 유틸 / 디버그 =======================

    private static Map<String,String> collectHeaders(HttpServletRequest req) {
        Map<String,String> m = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Enumeration<String> e = req.getHeaderNames();
        while (e != null && e.hasMoreElements()) {
            String k = e.nextElement();
            m.put(k, req.getHeader(k));
        }
        return m;
    }

    private static String safeHeaderPreview(Map<String,String> h) {
        Map<String,String> m = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (var e : h.entrySet()) {
            String k = e.getKey();
            String v = e.getValue();
            if (k.equalsIgnoreCase("authorization")) v = "***";
            if (k.equalsIgnoreCase("webhook-signature")) v = maskSignature(v);
            m.put(k, v);
        }
        return m.toString();
    }

    private static String maskSignature(String s) {
        if (s == null) return null;
        if (s.length() <= 12) return "****";
        return s.substring(0, 6) + "..." + s.substring(s.length()-6);
    }

    private static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e){ return "NA"; }
    }

    private static boolean isBlank(String s){ return s == null || s.isBlank(); }
    private static String nz(String s){ return s == null ? "" : s; }

    /** 여러 문자열 중 처음으로 비어있지 않은 값 반환 */
    private static String firstNonBlank(String... v) {
        if (v == null) return null;
        for (String s : v) {
            if (s != null && !s.isBlank()) {
                return s;
            }
        }
        return null;
    }

    /** 포트원 표준 시그니처: "t=<timestamp>,v1=<base64(hmac(id.timestamp.body))>" */
    private static class DebugVerifyResult {
        String tVal;
        String expectedBase64Preview;
        String v1Preview;
        boolean secretBase64Ok;
        boolean skewOk;
    }

    private static DebugVerifyResult debugVerifyManual(String secret, String id, String ts, String sigHeader, String body) {
        DebugVerifyResult r = new DebugVerifyResult();
        try {
            // 1) signature 파싱
            String t = null, v1 = null;
            if (sigHeader != null) {
                Matcher mt = Pattern.compile("(^|,)\\s*t=([^,]+)").matcher(sigHeader);
                if (mt.find()) t = mt.group(2).trim();
                Matcher mv = Pattern.compile("(^|,)\\s*v1=([^,]+)").matcher(sigHeader);
                if (mv.find()) v1 = mv.group(2).trim();
            }
            r.tVal = t;
            r.v1Preview = preview(v1);

            // 2) 시크릿 처리 (whsec_...는 base64)
            byte[] key;
            try {
                String base = secret.startsWith("whsec_") ? secret.substring(6) : secret;
                key = Base64.getDecoder().decode(base);
                r.secretBase64Ok = true;
            } catch (Throwable e) {
                key = secret.getBytes(StandardCharsets.UTF_8);
                r.secretBase64Ok = false;
            }

            // 3) 메시지 = id + "." + ts + "." + body
            String msg = nz(id) + "." + nz(ts) + "." + (body == null ? "" : body);

            // 4) HMAC-SHA256 → Base64
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            String expected = Base64.getEncoder().encodeToString(mac.doFinal(msg.getBytes(StandardCharsets.UTF_8)));
            r.expectedBase64Preview = preview(expected);

            // 5) 타임스탬프 스큐(±10분) 체크(디버그용 느슨)
            try {
                long now = System.currentTimeMillis() / 1000L;
                long tsec = Long.parseLong(nz(t));
                r.skewOk = Math.abs(now - tsec) < 600;
            } catch (Throwable ignore) {}

        } catch (Exception e) {
            log.warn("[WEBHOOK/DEBUG] manual verify exception: {}", e.toString());
        }
        return r;
    }
    private static String preview(String s) {
        if (s == null) return null;
        if (s.length() <= 10) return s;
        return s.substring(0, 6) + "..." + s.substring(s.length()-6);
    }

    // (선택) 외부 접근/상태 확인용
    @GetMapping("/health")
    public String health() { return "OK"; }
}
