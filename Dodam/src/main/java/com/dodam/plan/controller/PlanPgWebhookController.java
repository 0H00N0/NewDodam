package com.dodam.plan.controller;

import com.dodam.plan.webhook.PlanWebhookProcessingService;
import com.dodam.plan.webhook.PlanWebhookSignVerifier;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/webhooks/pg")
@RequiredArgsConstructor
public class PlanPgWebhookController {

    private final PlanWebhookSignVerifier signVerifier;
    private final PlanWebhookProcessingService processingService;

    private static final JsonMapper M = JsonMapper.builder().build();

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> handle(@RequestHeader Map<String, String> headers,
                                    @RequestBody String raw) {
        try {
            // 1) 서명 검증 (Webhook-Signature: v1,<base64>)
            if (!signVerifier.verify(headers, raw)) {
                log.warn("[WEBHOOK] invalid signature. headers={}", headers);
                // 보안상 401로 거절할 수도 있지만, 재시도폭주를 피하려면 200으로 흡수해도 무방
                return ResponseEntity.ok().body("invalid signature");
            }

            // 2) 최소한의 파싱만 하고 즉시 비동기 처리 위임
            JsonNode root = M.readTree(raw);

            // 이벤트/결제ID/트랜잭션ID/상태 추출(없어도 괜찮게 설계 – 비동기에서 재파싱/조회함)
            String type        = asText(root, "event", "type");
            String paymentId   = firstNonBlank(
                    asText(root, "paymentId", "payment_id", "id", "payment.id"),
                    // 일부 페이로드는 data.payment.id 형태
                    asText(root.path("data").path("payment"), "id", "paymentId", "payment_id")
            );
            String transaction = firstNonBlank(
                    asText(root, "transactionUid", "transaction_uid", "tx_id"),
                    asText(root.path("data").path("payment"), "txId", "transactionId", "transaction_id")
            );
            String status      = firstNonBlank(
                    asText(root, "status", "payment.status", "pay.status"),
                    asText(root.path("data").path("payment"), "status")
            );

            log.info("[WEBHOOK] enqueue type={}, paymentId={}, txId={}, status={}", type, paymentId, transaction, status);
            processingService.process(type, paymentId, transaction, status, raw); // ★ 비동기

            // 3) 절대 지연 없이 OK 반환
            return ResponseEntity.ok("ok");
        } catch (Exception e) {
            log.error("[WEBHOOK] error", e);
            // 에러여도 200으로 받아주고 내부 알람으로 처리해도 됨
            return ResponseEntity.ok("ok");
        }
    }

    /* -------------- helpers (controller 경량 파서) -------------- */

    private static String asText(JsonNode root, String... keys) {
        if (root == null || keys == null) return null;
        for (String k : keys) {
            JsonNode n = getByDotted(root, k);
            if (n != null && !n.isMissingNode() && !n.isNull() && n.isValueNode()) {
                String v = n.asText(null);
                if (StringUtils.hasText(v)) return v;
            }
        }
        return null;
    }
    private static JsonNode getByDotted(JsonNode root, String dotted) {
        String[] parts = dotted.split("\\.");
        JsonNode cur = root;
        for (String p : parts) {
            if (cur == null) return null;
            cur = cur.get(p);
        }
        return cur;
    }
    private static String firstNonBlank(String... v){
        if (v == null) return null;
        for (String s : v) if (StringUtils.hasText(s)) return s;
        return null;
    }
}
