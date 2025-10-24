package com.dodam.config;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.web.csrf.CsrfToken;

import java.util.Map;

@RestController
public class CsrfController {

    /**
     * 호출 시 세션에 CSRF 토큰을 생성하고 값을 내려준다.
     * 프런트는 여기서 받은 token을 "X-XSRF-TOKEN" 헤더로 보낸다.
     */
    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of(
            "headerName", token.getHeaderName(),     // "X-XSRF-TOKEN"
            "parameterName", token.getParameterName(),
            "token", token.getToken()
        );
    }
}
