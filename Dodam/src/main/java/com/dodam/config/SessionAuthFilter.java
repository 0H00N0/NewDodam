package com.dodam.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Component
public class SessionAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SessionAuthFilter.class);

    /** 이 필터를 완전히 우회할 경로 prefix들 */
    private static final Set<String> BYPASS_PREFIXES = Set.of(
        "/webhooks/pg",      // ★ PortOne 웹훅
        "/pg/",              // (혹시 쓰는 PG 라우팅)
        "/payments/",        // 결제 확인/조회
        "/reviews/",		//리뷰 공개조회
        "/subscriptions/",   // 구독 시작/조회
        "/billing-keys/",    // 빌링키 등록/확정
        "/h2-console/",      // 콘솔
        "/static/", "/assets/", "/favicon", "/css/", "/js/", "/img/", "/images/"
    );

    private static boolean shouldBypass(HttpServletRequest req) {
        String uri = req.getRequestURI();
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) return true; // CORS preflight은 항상 우회
        if (uri == null || uri.isEmpty()) return false;
        for (String p : BYPASS_PREFIXES) {
            if (uri.startsWith(p)) return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        // ★ 웹훅/PG/빌링/구독/정적/OPTIONS 요청은 전부 우회 (세션/인증 세팅 절대 금지)
        if (shouldBypass(req)) {
            chain.doFilter(req, res);
            return;
        }

        // 이미 인증 객체가 세팅되어 있지 않은 경우에만 진행
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            // 세션을 새로 만들지 않음
            HttpSession session = req.getSession(false);
            if (session != null) {
                Object sid  = session.getAttribute("sid");   // 로그인한 사용자 아이디
                Object role = session.getAttribute("role");  // ROLE_ADMIN / ROLE_USER

                // 필요 시만 디버그 로그 (운영에서는 INFO 이하로)
                if (log.isDebugEnabled()) {
                    log.debug("SessionAuthFilter sid={}, role={}", sid, role);
                }

                if (sid != null) {
                    // 세션에 role이 없으면 기본 USER로 처리
                    String roleName = (role instanceof String s && !s.isBlank()) ? s : "ROLE_USER";

                    var auth = new UsernamePasswordAuthenticationToken(
                            sid,               // principal (사용자 ID)
                            null,              // credentials (사용하지 않음)
                            List.of(new SimpleGrantedAuthority(roleName)) // 권한 부여
                    );

                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }

        chain.doFilter(req, res);
    }
}
