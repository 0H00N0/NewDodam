// src/main/java/com/dodam/plan/webhook/PlanWebhookSignVerifier.java
package com.dodam.plan.webhook;

import com.dodam.plan.config.PlanPortoneProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;

@Component
public class PlanWebhookSignVerifier {

    private final PlanPortoneProperties props;

    public PlanWebhookSignVerifier(PlanPortoneProperties props) {
        this.props = props;
    }

    /**
     * 헤더 예시:
     *   Webhook-Signature: v1,<base64(hmac-sha256(rawBody, webhook.secret))>
     * 포트원/프록시 환경에 따라 대소문자나 헤더명이 변형될 수 있어 넓게 커버한다.
     */
    public boolean verify(Map<String, String> headers, String rawBody) {
        String secret = props.getWebhookSecret();
        if (secret == null || secret.isBlank()) return false;

        String header = pick(headers,
                "Webhook-Signature", "webhook-signature",
                "X-Portone-Signature", "x-portone-signature",
                "Portone-Signature", "portone-signature");
        if (header == null || !header.toLowerCase(Locale.ROOT).startsWith("v1,")) {
            return false;
        }
        String provided = header.substring(3).trim();
        String expected = hmacSha256Base64(rawBody, secret);
        return slowEquals(provided, expected);
    }

    private String pick(Map<String, String> headers, String... keys) {
        if (headers == null) return null;
        for (String k : keys) {
            if (headers.containsKey(k)) return headers.get(k);
        }
        // 스프링이 모두 소문자로 내려주는 케이스 방어
        for (String k : keys) {
            String low = k.toLowerCase(Locale.ROOT);
            if (headers.containsKey(low)) return headers.get(low);
        }
        return null;
        }

    private static String hmacSha256Base64(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] sig = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(sig);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean slowEquals(String a, String b) {
        if (a == null || b == null) return false;
        if (a.length() != b.length()) return false;
        int res = 0;
        for (int i = 0; i < a.length(); i++) res |= a.charAt(i) ^ b.charAt(i);
        return res == 0;
    }
}
