// src/main/java/com/dodam/plan/webhook/PlanWebhookAsyncConfig.java
package com.dodam.plan.webhook;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class PlanWebhookAsyncConfig {

    @Bean(name = "pgWebhookExecutor") // ← 이름 변경
    public Executor pgWebhookExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(4);
        ex.setMaxPoolSize(8);
        ex.setQueueCapacity(200);
        ex.setThreadNamePrefix("webhook-");
        ex.initialize();
        return ex;
    }
}
