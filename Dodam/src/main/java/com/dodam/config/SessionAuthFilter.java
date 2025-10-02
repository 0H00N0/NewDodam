package com.dodam.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class SessionAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        // 이미 인증 객체가 세팅되어 있지 않은 경우에만 진행
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            HttpSession session = req.getSession(false);
            if (session != null) {
                Object sid  = session.getAttribute("mid");   // 로그인한 사용자 아이디
                Object role = session.getAttribute("role");  // ROLE_ADMIN / ROLE_USER
                System.out.println(">>> SessionAuthFilter sid=" + sid + ", role=" + role);

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
