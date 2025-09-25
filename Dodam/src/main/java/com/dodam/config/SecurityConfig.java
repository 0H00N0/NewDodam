
package com.dodam.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final SessionAuthFilter sessionAuthFilter;

    @Value("${front.origin:http://localhost:3000}")
    private String front;

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
        	// CORS
            .cors(cors -> cors.configurationSource(corsSource()))
            // CSRF: 상태변경 엔드포인트만 예외
            .csrf(csrf -> csrf.ignoringRequestMatchers(
                // 기존
                "/oauth/**",
                "/member/loginForm",
                "/member/logout",
                "/member/updateProfile",
                "/member/changePw",
                "/member/changePwDirect",
                "/member/signup",
                "/webhooks/pg",
                // ✅ 결제/구독/빌링키 전부 예외 처리
                "/payments/**",
                "/subscriptions/**",
                "/billing-keys/**",
                "/pg/payments/**",
                "/pg/transactions/**",
                "/events/**",
                "/admin/**"
            ))
            // 인가
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/member/**").permitAll()
                .requestMatchers("/webhooks/pg").permitAll()
                .requestMatchers("/payments/**").permitAll()
                .requestMatchers("/pg/payments/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/pg/payments/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/pg/transactions/**").permitAll()
                .requestMatchers("/subscriptions/**").permitAll()
                .requestMatchers("/billing-keys/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/billing-keys/confirm", "/billing-keys/register").permitAll()
                .requestMatchers(HttpMethod.GET,  "/billing-keys/list").permitAll()
                .requestMatchers("/sub/**").authenticated()
                .requestMatchers("/pg/**").authenticated()
                .requestMatchers("/static/**").permitAll()
                .requestMatchers("/oauth/**").permitAll()
                .requestMatchers("/favicon.ico").permitAll()
                .requestMatchers("/member/signup").permitAll()
                .requestMatchers("/member/loginForm").permitAll()
                .requestMatchers("/products/**").permitAll()
                .requestMatchers("/index.html").permitAll()
                .requestMatchers("/admin/**").permitAll()
                .requestMatchers("/events/**").permitAll()
                .anyRequest().permitAll()
            )
            // 세션 기반
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            // 세션 인증 주입 필터
            .addFilterBefore(sessionAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsSource() {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowCredentials(true); // axios withCredentials:true 매칭
        c.setAllowedOrigins(List.of(
            front,                      // ex) http://localhost:3000
            "http://127.0.0.1:3000"
        ));
        c.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
        c.setAllowedHeaders(List.of("*"));
        // 필요시 노출헤더: c.setExposedHeaders(List.of("Set-Cookie"));

        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", c);
        return src;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
