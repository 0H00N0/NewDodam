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
                "/admin/**", 
                "/cart/**",
                "/rent/**",
                "/test/**",
                "/product-inquiries/**",
                "/member/findPwByMemail",
                "/member/findPwByMtel"
            ))
            // 인가 정책
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
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
                .requestMatchers("/test/**").permitAll()   // ← 테스트용 개방
                .requestMatchers("/rent/**").authenticated()
                // ✅ 추가(권장): 상품 문의는 로그인 필요
                .requestMatchers("/product-inquiries/**").authenticated()
                // ✅ 게시판은 로그인 필요 (USER/ADMIN 모두)
                .requestMatchers("/board/**").authenticated()

                // ✅ 관리자 전용
                .requestMatchers("/admin/**").hasRole("ADMIN")

                // 상품/리뷰 등 공개 API
                .requestMatchers(HttpMethod.GET, "/api/products/new", "/api/products/popular").permitAll()
                .requestMatchers("/api/products/**", "/api/reviews/count").permitAll()

                // 그 외 기본은 전부 허용
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
        c.setAllowCredentials(true); 
        c.setAllowedOrigins(List.of(
            front,
            "http://127.0.0.1:3000"
        ));
        c.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        c.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", c);
        return src;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
