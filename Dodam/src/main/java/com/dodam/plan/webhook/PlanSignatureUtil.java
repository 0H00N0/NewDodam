package com.dodam.plan.webhook;

import lombok.experimental.UtilityClass;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@UtilityClass
public class PlanSignatureUtil {

    // 후보 헤더 키(대소문자 무시). PortOne 문서 스펙에 맞춰 필요 시 조정
    private static final String[] CANDIDATE_SIG_HEADERS = {
            "x-portone-signature", "x-iamport-signature",
            "x-signature", "x-webhook-signature", "portone-signature"
    };

    public static String findSignatureHeader(Map<String, String> headers) {
        if (headers == null) return null;
        for (String key : headers.keySet()) {
            String lower = key.toLowerCase();
            for (String cand : CANDIDATE_SIG_HEADERS) {
                if (lower.equals(cand)) return headers.get(key);
            }
        }
        return null;
    }

    /** HMAC-SHA256(body, secret) → Base64 텍스트 */
    public static String hmacBase64(String body, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(raw);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC compute failed", e);
        }
    }

    /** 타이밍 공격 방지 비교 */
    public static boolean constantTimeEq(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int r = 0; for (int i = 0; i < a.length(); i++) r |= a.charAt(i) ^ b.charAt(i);
        return r == 0;
    }
}
