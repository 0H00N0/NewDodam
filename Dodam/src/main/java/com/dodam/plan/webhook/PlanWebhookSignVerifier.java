// src/main/java/com/dodam/plan/webhook/PlanWebhookSignVerifier.java
package com.dodam.plan.webhook;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

public final class PlanWebhookSignVerifier {
    private PlanWebhookSignVerifier() {}

    /** Webhook-Signature: "v1,<base64>", HMAC-SHA256(rawBody) */
    public static boolean verifyV1(String providedHeader, String secret, String rawBody) {
        if (providedHeader == null || !providedHeader.startsWith("v1,") || secret == null || secret.isBlank()) {
            return false;
        }
        String provided = providedHeader.substring(3).trim();
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] sig = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            String expected = Base64.getEncoder().encodeToString(sig);
            return slowEquals(provided, expected);
        } catch (Exception e) {
            return false;
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
