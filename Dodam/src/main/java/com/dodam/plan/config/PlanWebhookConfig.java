package com.dodam.plan.config;

import com.dodam.plan.webhook.PlanWebhookSignVerifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PlanPortoneProperties.class)
public class PlanWebhookConfig {

    @Bean
    public PlanWebhookSignVerifier planWebhookSignVerifier(PlanPortoneProperties props) {
        return new PlanWebhookSignVerifier(props);
    }
}
