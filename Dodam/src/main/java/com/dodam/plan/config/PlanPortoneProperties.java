// src/main/java/com/dodam/plan/config/PlanPortoneProperties.java
package com.dodam.plan.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter @Setter
@ConfigurationProperties(prefix = "portone")
public class PlanPortoneProperties {
    private String baseUrl;     // e.g. https://api.portone.io
    private String v2Secret;    // Authorization: PortOne {v2Secret}
    private String storeId;
    private String channelKey;  // SDK 전용(서버 API에선 사용X)
    private String currency = "KRW";
    private Boolean isTest = false;

    public String authHeader() { return "PortOne " + v2Secret; }
}
