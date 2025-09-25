package com.dodam.oauth;

import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import com.dodam.member.entity.MemberEntity;
import com.dodam.member.entity.LoginmethodEntity;
import com.dodam.member.entity.MemtypeEntity;
import com.dodam.member.repository.MemberRepository;
import com.dodam.member.repository.LoginmethodRepository;
import com.dodam.member.repository.MemtypeRepository;

import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@RestController
@RequestMapping("/oauth")
@RequiredArgsConstructor
public class OAuthController {

    private final MemberRepository memberRepository;
    private final LoginmethodRepository loginmethodRepository;
    private final MemtypeRepository memtypeRepository;
    private final PasswordEncoder passwordEncoder;

    // ✅ 주입 받는 RestTemplate (AppConfig에 @Bean 추가 필요)
    private final RestTemplate restTemplate;

    // ====== application.properties / yml에서 주입 ======
    @Value("${oauth.kakao.rest-key}")
    private String kakaoRestKey;
    @Value("${oauth.kakao.redirect-uri}")
    private String kakaoRedirectUri;
    @Value("${oauth.kakao.client-secret:}")
    private String kakaoClientSecret;

    @Value("${oauth.naver.client-id}")
    private String naverClientId;
    @Value("${oauth.naver.client-secret}")
    private String naverClientSecret;
    @Value("${oauth.naver.redirect-uri}")
    private String naverRedirectUri;

    @PostMapping(
        value = "/{provider}/token",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> verifyAndLogin(
            @PathVariable("provider") String provider,
            @RequestBody Map<String, String> body,
            HttpSession session
    ) {
        final String providerLower = provider.toLowerCase(Locale.ROOT);
        final String providerUpper = providerLower.toUpperCase(Locale.ROOT);

        String token = body.get("token");
        String code  = body.get("code");
        String state = body.get("state"); // (naver)

        // code -> access_token 교환
        if ((token == null || token.isBlank()) && code != null && !code.isBlank()) {
            token = switch (providerLower) {
                case "kakao" -> exchangeKakaoCodeForToken(code);
                case "naver" -> exchangeNaverCodeForToken(code, state);
                default -> null;
            };
        }
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "token or code is required");
        }

        // 사용자 정보 호출
        Map<String, Object> userinfo = switch (providerLower) {
            case "kakao" -> fetchKakaoUser(token);
            case "naver" -> fetchNaverUser(token);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unsupported provider");
        };

        String uid         = extractUid(providerLower, userinfo);
        String displayName = extractName(providerLower, userinfo);
        String email       = extractEmail(providerLower, userinfo);

        // ✅ 전화번호는 재대입 금지(람다에서 쓰므로), 미리 확정값으로
        String telRaw = extractPhone(providerLower, userinfo);
        final String telFinal = (telRaw == null || telRaw.isBlank()) ? "00000000000" : telRaw;

        final String mid = providerLower + ":" + uid;

        MemberEntity member = memberRepository.findByMid(mid).orElseGet(() -> {
            LoginmethodEntity lm = loginmethodRepository.findByLmtype(providerUpper)
                    .orElseGet(() -> loginmethodRepository.save(
                            LoginmethodEntity.builder().lmtype(providerUpper).build()
                    ));

            MemtypeEntity mt = memtypeRepository.findByMtcode(0)
                    .orElseGet(() -> memtypeRepository.save(
                            MemtypeEntity.builder().mtcode(0).mtname("일반").build()
                    ));

            String randomPwHash = passwordEncoder.encode(UUID.randomUUID().toString());

            MemberEntity created = MemberEntity.builder()
                    .mid(mid)
                    .mpw(randomPwHash)
                    .mname(displayName != null ? displayName : providerUpper + "사용자")
                    .memail(email)          // null 허용 가능
                    .mtel(telFinal)         // ✅ ORA-01400 회피용 기본값 포함
                    .loginmethod(lm)
                    .memtype(mt)
                    .build();
            return memberRepository.save(created);
        });

        // 세션 로그인
        session.setAttribute("sid", member.getMid());

        return ResponseEntity.ok(Map.of(
                "login", true,
                "mid", member.getMid(),
                "name", member.getMname(),
                "email", member.getMemail()
        ));
    }

    @GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> me(HttpSession session) {
        String sid = (session != null) ? (String) session.getAttribute("sid") : null;

        // Map.of 는 null 금지 → LinkedHashMap 사용 + null 값은 put 하지 않음
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("login", sid != null);
        if (sid != null) body.put("sid", sid);

        return ResponseEntity.ok(body);
    }


    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("logout", true));
    }

    // ================= Provider API =================

    @SuppressWarnings("rawtypes")
    private Map<String, Object> fetchKakaoUser(String accessToken) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(accessToken);
        HttpEntity<Void> http = new HttpEntity<>(h);
        ResponseEntity<Map> res = restTemplate.exchange(
                "https://kapi.kakao.com/v2/user/me",
                HttpMethod.GET, http, Map.class);
        return res.getBody();
    }

    @SuppressWarnings("rawtypes")
    private Map<String, Object> fetchNaverUser(String accessToken) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(accessToken);
        HttpEntity<Void> http = new HttpEntity<>(h);
        ResponseEntity<Map> res = restTemplate.exchange(
                "https://openapi.naver.com/v1/nid/me",
                HttpMethod.GET, http, Map.class);
        return res.getBody();
    }

    // ================= code → token =================

    @SuppressWarnings("rawtypes")
    private String exchangeKakaoCodeForToken(String code) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", kakaoRestKey);
        form.add("redirect_uri", kakaoRedirectUri);
        form.add("code", code);
        if (kakaoClientSecret != null && !kakaoClientSecret.isBlank()) {
            form.add("client_secret", kakaoClientSecret);
        }
        HttpEntity<MultiValueMap<String,String>> req = new HttpEntity<>(form, h);
        ResponseEntity<Map> res = restTemplate.postForEntity("https://kauth.kakao.com/oauth/token", req, Map.class);
        Map body = res.getBody();
        if (body == null || body.get("access_token") == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "kakao token exchange failed");
        }
        return String.valueOf(body.get("access_token"));
    }

    @SuppressWarnings("rawtypes")
    private String exchangeNaverCodeForToken(String code, String state) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", naverClientId);
        form.add("client_secret", naverClientSecret);
        form.add("redirect_uri", naverRedirectUri);
        form.add("code", code);
        if (state != null) form.add("state", state);
        HttpEntity<MultiValueMap<String,String>> req = new HttpEntity<>(form, h);
        ResponseEntity<Map> res = restTemplate.postForEntity("https://nid.naver.com/oauth2.0/token", req, Map.class);
        Map body = res.getBody();
        if (body == null || body.get("access_token") == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "naver token exchange failed");
        }
        return String.valueOf(body.get("access_token"));
    }

    // ================= 파서 =================

    @SuppressWarnings({"rawtypes","unchecked"})
    private String extractUid(String provider, Map<String, Object> u) {
        if ("kakao".equals(provider)) {
            Object id = u.get("id");
            if (id == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "no kakao id");
            return String.valueOf(id);
        } else {
            Map r = (Map) u.get("response");
            if (r == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "no naver response");
            Object id = r.get("id");
            if (id == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "no naver id");
            return String.valueOf(id);
        }
    }

    @SuppressWarnings({"rawtypes","unchecked"})
    private String extractName(String provider, Map<String, Object> u) {
        if ("kakao".equals(provider)) {
            Map<String, Object> kakaoAccount = (Map<String, Object>) u.get("kakao_account");
            if (kakaoAccount != null) {
                Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                if (profile != null) return (String) profile.get("nickname");
            }
            return null;
        } else {
            Map r = (Map) u.get("response");
            return r != null ? (String) r.get("name") : null;
        }
    }

    @SuppressWarnings({"rawtypes","unchecked"})
    private String extractEmail(String provider, Map<String, Object> u) {
        if ("kakao".equals(provider)) {
            Map<String, Object> kakaoAccount = (Map<String, Object>) u.get("kakao_account");
            return kakaoAccount != null ? (String) kakaoAccount.get("email") : null;
        } else {
            Map r = (Map) u.get("response");
            return r != null ? (String) r.get("email") : null;
        }
    }

    @SuppressWarnings({"rawtypes","unchecked"})
    private String extractPhone(String provider, Map<String, Object> u) {
        try {
            if ("kakao".equals(provider)) {
                Map<String, Object> kakaoAccount = (Map<String, Object>) u.get("kakao_account");
                if (kakaoAccount != null) {
                    Object phone = kakaoAccount.get("phone_number"); // "+82 10-1234-5678"
                    if (phone != null) return String.valueOf(phone).replaceAll("\\D", "");
                }
                return null;
            } else {
                Map r = (Map) u.get("response");
                if (r != null) {
                    Object e164 = r.get("mobile_e164"); // +8210...
                    if (e164 != null) return String.valueOf(e164).replaceAll("\\D", "");
                    Object mobile = r.get("mobile");     // 010-1234-5678
                    if (mobile != null) return String.valueOf(mobile).replaceAll("\\D", "");
                }
                return null;
            }
        } catch (Exception ignore) {
            return null;
        }
    }
}
