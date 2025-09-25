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
import reactor.netty.http.client.HttpClient;

@Slf4j
@Configuration
@EnableConfigurationProperties(PlanPortoneProperties.class)
public class PlanPortoneConfig {

    @Bean("portoneWebClient")
    public WebClient portoneWebClient(PlanPortoneProperties props) {
        HttpClient hc = HttpClient.create()
            .responseTimeout(Duration.ofSeconds(60)) // confirm 여유
            .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
            .doOnConnected(conn -> conn
                .addHandlerLast(new io.netty.handler.timeout.ReadTimeoutHandler(60))
                .addHandlerLast(new io.netty.handler.timeout.WriteTimeoutHandler(60)));

        return WebClient.builder()
            .baseUrl(props.getBaseUrl())
            .defaultHeader("Authorization", props.authHeader()) // ✅ V2 인증 고정
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
