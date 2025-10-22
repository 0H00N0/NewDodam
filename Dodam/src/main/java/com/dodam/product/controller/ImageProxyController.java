package com.dodam.product.controller;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.io.ByteArrayResource;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/image")
@CrossOrigin(origins = "http://localhost:3000")
public class ImageProxyController {

    private final RestTemplate rest = new RestTemplate();

    @GetMapping("/proxy")
    public ResponseEntity<ByteArrayResource> proxy(@RequestParam("url") String url) {
        try {
            ResponseEntity<byte[]> resp = rest.exchange(url, HttpMethod.GET, null, byte[].class);
            HttpHeaders headers = new HttpHeaders();
            MediaType contentType = resp.getHeaders().getContentType();
            headers.setContentType(contentType != null ? contentType : MediaType.IMAGE_JPEG);
            headers.setCacheControl(CacheControl.maxAge(1, TimeUnit.HOURS));
            return new ResponseEntity<>(new ByteArrayResource(resp.getBody()), headers, resp.getStatusCode());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }
}