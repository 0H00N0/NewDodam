// src/main/java/com/dodam/plan/config/PortoneTokenService.java
package com.dodam.plan.service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.dodam.plan.config.PlanPortoneProperties;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PlanPortoneTokenService {

    private static final String AUTH_URL = "https://api.portone.io/login/api-secret";

    private record Token(String accessToken, String refreshToken, Instant expiresAt) {}

    private final PlanPortoneProperties props;
    private final WebClient.Builder webClientBuilder;
    private final AtomicReference<Token> cache = new AtomicReference<>(null);

    public synchronized String getAccessToken() {
        Token t = cache.get();
        if (t == null || t.expiresAt.isBefore(Instant.now().plusSeconds(30))) {
            t = issueByApiSecret();
            cache.set(t);
        }
        return t.accessToken();
    }

    public synchronized void forceRefresh() {
        cache.set(issueByApiSecret());
    }

    @SuppressWarnings("unchecked")
    private Token issueByApiSecret() {
        WebClient client = webClientBuilder.baseUrl(AUTH_URL).build();
        Map<String, Object> res = client.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("apiSecret", props.getV2Secret()))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (res == null || res.get("accessToken") == null) {
            throw new IllegalStateException("PortOne accessToken 발급 실패: " + res);
        }
        String at = (String) res.get("accessToken");
        String rt = (String) res.getOrDefault("refreshToken", "");
        // accessToken 30분 유효 → 29분 캐시
        return new Token(at, rt, Instant.now().plusSeconds(29 * 60));
    }
}
