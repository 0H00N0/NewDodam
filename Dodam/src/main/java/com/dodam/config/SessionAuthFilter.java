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

import com.dodam.member.entity.MemberEntity;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Component
public class SessionAuthFilter extends OncePerRequestFilter {

    private static final Set<String> BYPASS_PREFIXES = Set.of(
        "/webhooks/pg", "/pg/", "/payments/", "/subscriptions/", "/billing-keys/",
        "/h2-console/", "/static/", "/assets/", "/favicon", "/css/", "/js/", "/img/", "/images/"
    );

    private static boolean shouldBypass(HttpServletRequest req) {
        String uri = req.getRequestURI();
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) return true;
        if (uri == null) return false;
        for (String p : BYPASS_PREFIXES) if (uri.startsWith(p)) return true;

        // ✅ 커뮤니티 열람은 비로그인도 통과
        if ("GET".equalsIgnoreCase(req.getMethod())) {
            if (uri.equals("/board/community")) return true;
            if (uri.matches("^/board/community/\\d+$")) return true;
            if (uri.matches("^/board/community/\\d+/comments$")) return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        if (shouldBypass(req)) {
            chain.doFilter(req, res);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            HttpSession session = req.getSession(false);
            if (session != null) {
                Object sid  = session.getAttribute("sid");
                Object role = session.getAttribute("role");
                Object login = session.getAttribute("loginUser");

                if (sid == null && login instanceof MemberEntity m) {
                    sid = m.getMid();
                    if (role == null) role = "ROLE_USER";
                }
                if (sid != null) {
                    String roleName = (role instanceof String s && !s.isBlank()) ? s : "ROLE_USER";
                    var auth = new UsernamePasswordAuthenticationToken(
                        sid, null, List.of(new SimpleGrantedAuthority(roleName))
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }
        chain.doFilter(req, res);
    }
}