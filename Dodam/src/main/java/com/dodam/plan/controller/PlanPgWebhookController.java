package com.dodam.plan.controller;

import com.dodam.plan.config.PlanPortoneProperties;
import com.dodam.plan.webhook.PlanWebhookProcessingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.portone.sdk.server.webhook.*;
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

    private final PlanPortoneProperties props;
    private final PlanWebhookProcessingService processingSvc;
    private final ObjectMapper mapper = new ObjectMapper();

    @PostMapping(value = "/pg", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> handlePortoneWebhook(
            HttpServletRequest req,
            @RequestBody(required = false) byte[] rawBytes
    ) {
        String raw = (rawBytes == null) ? "" : new String(rawBytes, StandardCharsets.UTF_8);

        // ── 수신 로그 요약 ────────────────────────
        String ct = nz(req.getContentType());
        String sha256 = sha256Hex(raw);
        Map<String, String> headersAll = collectHeaders(req);
        log.info("[WEBHOOK/IN] ct={} body.len={} sha256={} headers={}",
                ct, raw.length(), sha256, safeHeaderPreview(headersAll));

        // ── 필수 헤더 추출 ─────────────────────────
        String id  = firstNonBlank(
                req.getHeader(WebhookVerifier.HEADER_ID),
                req.getHeader("Webhook-Id"),
                req.getHeader("X-Webhook-Id")
        );
        String sig = firstNonBlank(
                req.getHeader(WebhookVerifier.HEADER_SIGNATURE),
                req.getHeader("Webhook-Signature"),
                req.getHeader("X-Webhook-Signature")
        );
        String ts  = firstNonBlank(
                req.getHeader(WebhookVerifier.HEADER_TIMESTAMP),
                req.getHeader("Webhook-Timestamp"),
                req.getHeader("X-Webhook-Timestamp")
        );

        if (isBlank(id) || isBlank(sig) || isBlank(ts)) {
            log.warn("[WEBHOOK/ERR] missing headers id={} sig.len={} ts={}",
                    id, sig == null ? null : sig.length(), ts);
            return ResponseEntity.badRequest().body("missing headers");
        }

        // ── 서명 검증 ───────────────────────────────
        String secret = nz(props.getWebhookSecret());
        Webhook event;
        try {
            WebhookVerifier verifier = new WebhookVerifier(secret);
            event = verifier.verify(raw, id, sig, ts);
        } catch (Exception ex) {
            log.warn("[WEBHOOK/VERIFY] SDK failed: {}", ex.toString());
            DebugVerifyResult dbg = debugVerifyManual(secret, id, ts, sig, raw);
            log.warn("[WEBHOOK/DEBUG] manual expectedBase64={} actual.v1={} id={} ts={} t={} skewOk={} secretBase64Ok={}",
                    dbg.expectedBase64Preview, dbg.v1Preview, id, ts, dbg.tVal, dbg.skewOk, dbg.secretBase64Ok);
            return ResponseEntity.badRequest().body("invalid signature");
        }

        // ── raw → JsonNode 변환 (빌링키 이벤트에서 사용) ─────────────
        JsonNode rootJson;
        try {
            rootJson = mapper.readTree((raw == null || raw.isBlank()) ? "{}" : raw);
        } catch (Exception e) {
            rootJson = mapper.createObjectNode();
        }

        // ── 이벤트 타입별 분기 처리 ───────────────────────
        try {
            if (event instanceof WebhookTransactionPaid e) {
                var d = e.getData();
                String paymentId = nz(d.getPaymentId());
                String txId      = nz(d.getTransactionId());
                log.info("[WEBHOOK/OK] PAID paymentId={} txId={}", paymentId, txId);
                processingSvc.process("Transaction.Paid", paymentId, txId, "PAID", raw);

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

            } else if (event.getClass().getSimpleName().equals("WebhookBillingKeyReady")) {
                log.info("[WEBHOOK/BILLINGKEY] BillingKeyReady 수신됨");
                processingSvc.handleBillingKeyEvent(rootJson, false);

            } else if (event.getClass().getSimpleName().equals("WebhookBillingKeyIssued")) {
                log.info("[WEBHOOK/BILLINGKEY] BillingKeyIssued 수신됨");
                processingSvc.handleBillingKeyEvent(rootJson, true);

            } else {
                log.info("[WEBHOOK/OK] Unhandled type: {}", event.getClass().getSimpleName());
            }
        } catch (Exception ex) {
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
        return s.substring(0, 6) + "..." + s.substring(s.length() - 6);
    }

    private static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { return "NA"; }
    }

    private static boolean isBlank(String s){ return s == null || s.isBlank(); }
    private static String nz(String s){ return s == null ? "" : s; }

    private static String firstNonBlank(String... v) {
        if (v == null) return null;
        for (String s : v) {
            if (s != null && !s.isBlank()) return s;
        }
        return null;
    }

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
            String t = null, v1 = null;
            if (sigHeader != null) {
                Matcher mt = Pattern.compile("(^|,)\\s*t=([^,]+)").matcher(sigHeader);
                if (mt.find()) t = mt.group(2).trim();
                Matcher mv = Pattern.compile("(^|,)\\s*v1=([^,]+)").matcher(sigHeader);
                if (mv.find()) v1 = mv.group(2).trim();
            }
            r.tVal = t;
            r.v1Preview = preview(v1);

            byte[] key;
            try {
                String base = secret.startsWith("whsec_") ? secret.substring(6) : secret;
                key = Base64.getDecoder().decode(base);
                r.secretBase64Ok = true;
            } catch (Throwable e) {
                key = secret.getBytes(StandardCharsets.UTF_8);
                r.secretBase64Ok = false;
            }

            String msg = nz(id) + "." + nz(ts) + "." + (body == null ? "" : body);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            String expected = Base64.getEncoder().encodeToString(mac.doFinal(msg.getBytes(StandardCharsets.UTF_8)));
            r.expectedBase64Preview = preview(expected);

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
        return s.substring(0, 6) + "..." + s.substring(s.length() - 6);
    }

    @GetMapping("/health")
    public String health() { return "OK"; }
}
