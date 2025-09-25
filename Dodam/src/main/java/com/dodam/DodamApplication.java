package com.dodam;

import com.dodam.plan.config.PlanPortoneProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;


@SpringBootApplication
@EnableConfigurationProperties(PlanPortoneProperties.class) // ✅ properties 빈 등록
public class DodamApplication {

	/**
	 * 애플리케이션의 메인 메소드입니다.
	 * SpringApplication.run()을 호출하여 Spring 애플리케이션 컨텍스트를 초기화하고 실행합니다.
	 * @param args 커맨드 라인 인자
	 */
	public static void main(String[] args) {
		SpringApplication.run(DodamApplication.class, args);
	}
}

