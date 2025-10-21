package com.dodam.plan.controller;

import com.dodam.plan.service.PlanPaymentGatewayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/plan/attempts")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"}) // ← 추가
@Slf4j
public class PlanReceiptController {

    private final PlanPaymentGatewayService pgSvc;

    @GetMapping("/receipt")
    public ResponseEntity<?> getReceipt(@RequestParam Map<String,String> params) {
        // ✅ 자동타입변환 회피
        String invoiceId = trim(params.get("invoiceId"));
        String paymentId = trim(params.get("paymentId"));

        String pid = normalize(paymentId);

        // paymentId 없고 invoiceId만 있을 때: inv###-ts... → orderId로 역검색
        if (!StringUtils.hasText(pid) && StringUtils.hasText(invoiceId)) {
            try {
                String digits = invoiceId.replaceAll("[^0-9]", "");
                if (!digits.isBlank()) {
                    String orderId = "inv" + digits + "-ts";
                    var hit = pgSvc.findPaymentByExactOrderId(orderId);
                    if (hit.isPresent()) {
                        pid = hit.get().path("id").asText(null);
                    }
                }
            } catch (Exception ignore) {}
        }

        if (!StringUtils.hasText(pid)) {
            return ResponseEntity.ok(Map.of("url",""));
        }

        // === 게이트웨이 조회 ===
        var r = pgSvc.safeLookup(pid);
        String receipt = null;

        // 1) 필드 반사
        try {
            var f = r.getClass().getDeclaredField("receiptUrl");
            f.setAccessible(true);
            Object v = f.get(r);
            if (v != null && v.toString().startsWith("http")) receipt = v.toString();
        } catch (Exception ignore) {}

        // 2) rawJson 파싱
        if (!StringUtils.hasText(receipt)) {
            try {
                String raw = r.rawJson();
                if (raw != null && !raw.isBlank()) {
                    if (raw.contains("\"receipt\":{\"url\"")) {
                        receipt = raw.split("\"receipt\":\\{")[1]
                                     .split("\"url\"\\s*:\\s*\"")[1].split("\"")[0];
                    } else if (raw.contains("\"card\":{\"receiptUrl\"")) {
                        receipt = raw.split("\"card\":\\{")[1]
                                     .split("\"receiptUrl\"\\s*:\\s*\"")[1].split("\"")[0];
                    }
                }
            } catch (Exception ignore) {}
        }

        return ResponseEntity.ok(Map.of("url", StringUtils.hasText(receipt) ? receipt : ""));
    }

    @GetMapping("/receipt/redirect")
    public ResponseEntity<?> redirectReceipt(@RequestParam Map<String,String> params) {
        ResponseEntity<?> json = getReceipt(params);
        if (json.getBody() instanceof Map<?,?> m) {
            Object u = m.get("url");
            if (u != null && !String.valueOf(u).isBlank()) {
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create(String.valueOf(u)))
                        .build();
            }
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error","RECEIPT_NOT_FOUND"));
    }

    private static String normalize(String s) { return (s == null || s.isBlank()) ? null : s.trim(); }
    private static String trim(String s) { return s == null ? null : s.trim(); }
}
