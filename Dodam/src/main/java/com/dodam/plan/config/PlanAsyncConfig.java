// src/main/java/com/dodam/plan/config/PlanAsyncConfig.java
package com.dodam.plan.config;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class PlanAsyncConfig implements AsyncConfigurer {

    @Bean("paymentExecutor")
    public Executor paymentExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(4);
        ex.setMaxPoolSize(8);
        ex.setQueueCapacity(200);
        ex.setThreadNamePrefix("payment-");
        ex.initialize();
        return ex;
    }

    @Bean("webhookExecutor")
    public Executor webhookExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(4);
        ex.setMaxPoolSize(8);
        ex.setQueueCapacity(200);
        ex.setThreadNamePrefix("webhook-");
        ex.initialize();
        return ex;
    }

    /**
     * 비동기(@Async) 작업에서 던져진 예외가 삼켜지지 않고 로그에 찍히도록 처리
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new AsyncUncaughtExceptionHandler() {
            @Override
            public void handleUncaughtException(Throwable ex, Method method, Object... params) {
                LoggerFactory.getLogger(method.getDeclaringClass())
                        .error("[@Async] Uncaught exception in {} with params={}", 
                               method.getName(), params, ex);
            }
        };
    }
}
