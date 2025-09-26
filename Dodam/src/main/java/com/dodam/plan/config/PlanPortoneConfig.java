// src/main/java/com/dodam/plan/config/PlanPortoneConfig.java
package com.dodam.plan.config;

import lombok.extern.slf4j.Slf4j;
import java.time.Duration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import reactor.netty.http.client.HttpClient;

@Slf4j
@Configuration
@EnableConfigurationProperties(PlanPortoneProperties.class)
public class PlanPortoneConfig {

    // 네트워크/응답 타임아웃을 모두 60초로 통일
    private static final Duration TIMEOUT_NETWORK = Duration.ofSeconds(60);
    private static final int TIMEOUT_CONNECT_MS = 5_000;

    @Bean("portoneWebClient")
    public WebClient portoneWebClient(PlanPortoneProperties props) {
        HttpClient hc = HttpClient.create()
            // 서버 응답 대기시간(상단 레벨) - 15초
            .responseTimeout(TIMEOUT_NETWORK)
            // 커넥션 수립 타임아웃 - 5초
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, TIMEOUT_CONNECT_MS)
            // 소켓 레벨 read/write 타임아웃 - 15초
            .doOnConnected(conn -> conn
                .addHandlerLast(new ReadTimeoutHandler((int) TIMEOUT_NETWORK.getSeconds()))
                .addHandlerLast(new WriteTimeoutHandler((int) TIMEOUT_NETWORK.getSeconds())));

        return WebClient.builder()
            .baseUrl(props.getBaseUrl())
            .defaultHeader("Authorization", props.authHeader()) // ✅ V2 인증 고정 (예: "PortOne sk_live_xxx")
            .defaultHeader("Content-Type", "application/json")
            .clientConnector(new ReactorClientHttpConnector(hc))
            .filter(logRequest())
            .filter(logResponse())
            .build();
    }

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(req -> {
            log.info("[PortOne] {} {}  Authorization=PortOne ****", req.method(), req.url());
            return reactor.core.publisher.Mono.just(req);
        });
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(resp -> {
            log.debug("[PortOne] Response {} {}", resp.rawStatusCode(), resp.headers().asHttpHeaders());
            return reactor.core.publisher.Mono.just(resp);
        });
    }
}
