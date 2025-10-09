package com.dodam.plan.webhook;

import com.dodam.plan.config.PlanPortoneProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
public class PlanWebhookSignVerifier {

    private final PlanPortoneProperties props;

    // 표준 헤더 (Standard Webhooks)
    private static final String H_ID  = "webhook-id";
    private static final String H_TS  = "webhook-timestamp";
    private static final String H_SIG = "webhook-signature";

    // 과거/게이트웨이 상이 상황 호환용 후보들
    private static final String[] CANDIDATE_SIG_HEADERS = new String[]{
        "webhook-signature", "Webhook-Signature",
        "x-portone-signature", "X-Portone-Signature",
        "portone-signature", "Portone-Signature",
        "x-iamport-signature", "X-Iamport-Signature"
    };

    // v1=<base64(hmac)> 를 뽑아내는 정규식
    private static final Pattern P_V1 = Pattern.compile("(^|,)\\s*v1=([^,]+)");

    /** 표준 규격 검증: id + "." + timestamp + "." + rawBody */
    public boolean verify(Map<String, String> reqHeaders, String rawBody) {
        if (rawBody == null) rawBody = "";

        String secret = props.getWebhookSecret();
        if (secret == null || secret.isBlank()) {
            log.warn("[WEBHOOK] missing portone.webhook.secret");
            return false;
        }

        Map<String, String> headers = lowerCaseHeaders(reqHeaders);
        String id  = headers.get(H_ID);
        String ts  = headers.get(H_TS);
        String sig = headers.get(H_SIG);

        // 표준 헤더 3종이 모두 있는 경우 → 표준 방식으로 검증
        if (id != null && ts != null && sig != null) {
            String v1 = extractV1(sig);
            if (v1 == null) {
                log.warn("[WEBHOOK] signature header found but v1 part missing: {}", sig);
                return false;
            }
            String message = id + "." + ts + "." + rawBody;
            String expected = hmacSha256Base64(message, decodeWhsec(secret));
            boolean ok = constantTimeEq(v1, expected);
            if (!ok) {
                log.warn("[WEBHOOK] signature mismatch (std). v1.len={} expected.len={}", v1.length(), expected.length());
            }
            return ok;
        }

        // ── 호환: 표준 헤더가 아니면, 마지막 수단으로 구방식(rawBody만)도 시도 ──
        String fallback = pickAnySignature(headers);
        if (fallback != null) {
            // v1=... 또는 v1,... 모두 허용
            String v1 = extractV1(fallback);
            if (v1 == null && fallback.toLowerCase(Locale.ROOT).startsWith("v1,")) {
                v1 = fallback.substring(3).trim();
            }
            if (v1 != null) {
                String expected = hmacSha256Base64(rawBody, decodeWhsec(secret));
                boolean ok = constantTimeEq(v1, expected);
                if (!ok) {
                    log.warn("[WEBHOOK] signature mismatch (fallback). v1.len={} expected.len={}", v1.length(), expected.length());
                }
                return ok;
            }
        }

        log.warn("[WEBHOOK] required headers not found. got={}", headers.keySet());
        return false;
    }

    // 헤더 키 소문자 맵
    private static Map<String, String> lowerCaseHeaders(Map<String, String> src) {
        Map<String, String> m = new HashMap<>();
        if (src != null) {
            for (Map.Entry<String,String> e : src.entrySet()) {
                if (e.getKey() != null) m.put(e.getKey().toLowerCase(Locale.ROOT), e.getValue());
            }
        }
        return m;
    }

    private static String extractV1(String signatureHeader) {
        if (signatureHeader == null) return null;
        Matcher m = P_V1.matcher(signatureHeader);
        if (m.find()) return m.group(2).trim();
        return null;
    }

    private static String pickAnySignature(Map<String,String> headers) {
        for (String k : CANDIDATE_SIG_HEADERS) {
            String val = headers.get(k.toLowerCase(Locale.ROOT));
            if (val != null) return val;
        }
        return null;
    }

    private static byte[] decodeWhsec(String secret) {
        // 포트원 콘솔 시크릿은 보통 whsec_ 로 시작하며 Base64 (표준 규격). 디코딩 실패 시 원문 바이트 사용.
        try {
            String s = secret.startsWith("whsec_") ? secret.substring(6) : secret;
            return Base64.getDecoder().decode(s);
        } catch (Throwable ignore) {
            return secret.getBytes(StandardCharsets.UTF_8);
        }
    }

    private static String hmacSha256Base64(String message, byte[] key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            byte[] out = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean constantTimeEq(String a, String b) {
        if (a == null || b == null) return false;
        if (a.length() != b.length()) return false;
        int r = 0;
        for (int i=0; i<a.length(); i++) r |= a.charAt(i) ^ b.charAt(i);
        return r == 0;
    }
}
