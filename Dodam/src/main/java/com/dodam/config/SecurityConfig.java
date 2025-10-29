// src/main/java/com/dodam/config/SecurityConfig.java
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
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
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
            /** ✅ CORS 설정 */
            .cors(cors -> cors.configurationSource(corsSource()))

            /** ✅ CSRF 설정 (React와 쿠키 기반 통신 호환) */
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                .ignoringRequestMatchers(
                    "/oauth/**",
                    "/member/loginForm",
                    "/member/logout",
                    "/member/updateProfile",
                    "/member/changePw",
                    "/member/changePwDirect",
                    "/member/signup",
                    "/member/delete",
                    "/webhooks/pg",
                    "/payments/**",
                    "/subscriptions/**",
                    "/billing-keys/**",
                    "/pg/payments/**",
                    "/pg/transactions/**",
                    "/events/**",
                    "/pg/**",
                    "/api/products/new",
                    "/api/products/popular",
                    "/api/reviews/count",
                    "/api/products/**",
                    "/api/image/proxy",
                    "/admin/**",
                    "/cart/**",
                    "/rent/**",
                    "/test/**",
                    "/product-inquiries/**",
                    "/member/findPwByMemail",
                    "/member/findPwByMtel",
                    "/reviews/**"
                )
            )

            /** ✅ 권한 설정 */
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // Preflight 요청 허용
                .requestMatchers("/member/**").permitAll()
                .requestMatchers("/webhooks/pg").permitAll()
                .requestMatchers("/payments/**").permitAll()
                .requestMatchers("/pg/**").permitAll()
                .requestMatchers("/subscriptions/**").permitAll()
                .requestMatchers("/billing-keys/**").permitAll()
                .requestMatchers("/static/**").permitAll()
                .requestMatchers("/oauth/**").permitAll()
                .requestMatchers("/favicon.ico").permitAll()
                .requestMatchers("/products/**").permitAll()
                .requestMatchers("/index.html").permitAll()
                .requestMatchers("/events/**").permitAll()
                .requestMatchers("/test/**").permitAll()
                .requestMatchers("/api/image/proxy").permitAll()
                .requestMatchers("/reviews/**").permitAll()

                // ✅ 상품 문의, 게시판 등은 인증 필요
                .requestMatchers("/product-inquiries/**").authenticated()
                .requestMatchers("/board/**").authenticated()

                // ✅ 커뮤니티 읽기 허용
                .requestMatchers(HttpMethod.GET,
                    "/board/community",
                    "/board/community/**",
                    "/board/community/*/comments"
                ).permitAll()

                // ✅ 게시글 작성/수정/삭제는 로그인 필요
                .requestMatchers(HttpMethod.POST, "/board/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/board/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/board/**").authenticated()

                // ✅ CSRF 토큰 요청 허용
                .requestMatchers(HttpMethod.GET, "/csrf").permitAll()

                // ✅ 관리자 전용
                .requestMatchers("/admin/**").hasRole("ADMIN")

                // ✅ 공개 상품/리뷰 API
                .requestMatchers(HttpMethod.GET, "/api/products/new", "/api/products/popular").permitAll()
                .requestMatchers("/api/products/**", "/api/reviews/count").permitAll()

                .anyRequest().permitAll()
            )

            /** ✅ 세션 관리 */
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

            /** ✅ 커스텀 세션 인증 필터 추가 */
            .addFilterBefore(sessionAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** ✅ CORS 전역 설정 */
    @Bean
    CorsConfigurationSource corsSource() {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowCredentials(true);
        c.setAllowedOrigins(List.of(front, "http://localhost:3000"));
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of(
            "Origin", "Content-Type", "Accept",
            "Authorization", "X-XSRF-TOKEN"
        ));
        c.setExposedHeaders(List.of("Authorization", "Set-Cookie"));
        c.setMaxAge(3600L); // 캐싱 1시간
        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", c);
        return src;
    }

    /** ✅ 비밀번호 암호화 */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
