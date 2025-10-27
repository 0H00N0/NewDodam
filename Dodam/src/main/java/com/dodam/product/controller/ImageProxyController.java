package com.dodam.product.controller;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.io.ByteArrayResource;

import java.util.concurrent.TimeUnit;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/image")
@CrossOrigin(origins = "http://localhost:3000")
public class ImageProxyController {

    private final RestTemplate rest = new RestTemplate();

    @GetMapping("/proxy")
    public ResponseEntity<ByteArrayResource> proxy(@RequestParam("url") String url) {
        try {
            // ✅ 1) 한 번 디코딩해서 원본 URL 복원 (프론트/백 어디서 인코딩해도 안전)
            String target = URLDecoder.decode(url, StandardCharsets.UTF_8);

            // ✅ 2) UA/Accept 헤더 추가 (일부 CDN은 UA 없으면 403)
            HttpHeaders fwdHeaders = new HttpHeaders();
            fwdHeaders.set("User-Agent", "Mozilla/5.0");
            fwdHeaders.set("Accept", "*/*");
            // (정책상 필요 시) fwdHeaders.set("Referer", "https://원본-도메인");

            HttpEntity<Void> req = new HttpEntity<>(fwdHeaders);

            ResponseEntity<byte[]> resp = rest.exchange(
                target,
                HttpMethod.GET,
                req,
                byte[].class
            );

            // ✅ 3) 응답 헤더 구성 (Content-Type + 캐시)
            HttpHeaders out = new HttpHeaders();
            MediaType contentType = resp.getHeaders().getContentType();
            out.setContentType(contentType != null ? contentType : MediaType.IMAGE_JPEG);
            out.setCacheControl(CacheControl.maxAge(1, TimeUnit.HOURS));

            return new ResponseEntity<>(new ByteArrayResource(resp.getBody()), out, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }
}
