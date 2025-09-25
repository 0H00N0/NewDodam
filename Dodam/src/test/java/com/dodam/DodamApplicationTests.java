package com.dodam;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Spring Boot 애플리케이션 통합 테스트
 * 
 * <p>애플리케이션 컨텍스트가 정상적으로 로드되는지 확인합니다.</p>
 * 
 * @since 1.0.0
 */
@SpringBootTest
@ActiveProfiles("test")
class DodamApplicationTests {

	/**
	 * 애플리케이션 컨텍스트 로드 테스트
	 */
	@Test
	void contextLoads() {
		// Spring Boot 애플리케이션이 정상적으로 시작되는지 확인
		// 실제 테스트 코드는 없지만 컨텍스트 로드 자체가 테스트
	}

}
