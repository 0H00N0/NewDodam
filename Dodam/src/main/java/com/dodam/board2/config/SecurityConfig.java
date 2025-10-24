package com.dodam.board2.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // ✅ CSRF 보안 비활성화 (API 호출시 403 방지)
            .csrf(AbstractHttpConfigurer::disable)
            // ✅ 인증 없이 접근 허용
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/**").permitAll() // 모든 요청 허용
                .anyRequest().permitAll()
            )
            // ✅ 로그인, 로그아웃 폼 비활성화 (React에서 처리하므로)
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable);

        return http.build();
    }
}