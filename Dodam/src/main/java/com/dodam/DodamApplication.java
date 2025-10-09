package com.dodam; // ✅ 중요: com.dodam 처럼 최상위여야 하위 전부 스캔됨

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.dodam")          // 서비스/컨트롤러 스캔
@EnableJpaRepositories(basePackages = "com.dodam")  // 레포지토리 스캔
@EntityScan(basePackages = "com.dodam")             // 엔티티 스캔
public class DodamApplication {
    public static void main(String[] args) {
        SpringApplication.run(DodamApplication.class, args);
    }
}
