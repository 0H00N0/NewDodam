package com.dodam.plan.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter @Setter
@Configuration
@ConfigurationProperties(prefix = "payments.confirm")
public class PlanPaymentConfirmProperties {
    /** application.properties: payments.confirm.immediate.enabled=false */
    private boolean immediateEnabled = true;
}
