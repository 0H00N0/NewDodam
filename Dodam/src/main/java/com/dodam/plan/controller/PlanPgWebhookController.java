package com.dodam.plan.controller;

import com.dodam.plan.Entity.PlanInvoiceEntity;
import com.dodam.plan.repository.PlanInvoiceRepository;
import com.dodam.plan.repository.PlanPaymentRepository;
import com.dodam.plan.service.PlanBillingService;
import com.dodam.plan.service.PlanPaymentGatewayService;
import com.dodam.plan.service.PlanPortoneClientService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/webhooks/pg")
@RequiredArgsConstructor
public class PlanPgWebhookController {

    private final PlanInvoiceRepository invoiceRepo;
    private final PlanBillingService billingSvc;
    private final PlanPaymentGatewayService pgSvc;
    private final PlanPortoneClientService portoneClient; // orderId 조회용
    private final PlanPaymentRepository paymentRepo;      // 카드메타 갱신

    private static final JsonMapper M = JsonMapper.builder().build();
    private static final Set<String> PID_KEYS = setOf(
            "paymentId","payment_id","id","payment.id",
            "transactionUid","transaction_uid","tx_id",
            "orderId","order_id"
    );
    private static final Set<String> STATUS_KEYS  = setOf("status","payment.status","pay.status");
    private static final Set<String> RECEIPT_KEYS = setOf("receiptUrl","receipt.url","card.receiptUrl");

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<?> handle(@RequestBody String raw) {
        try {
            final JsonNode root = M.readTree(raw);
            log.info("[WEBHOOK RAW JSON PRETTY]\n{}", root.toPrettyString());

            final String anyId     = pickDeep(root, PID_KEYS);
            final String statusRaw = pickDeep(root, STATUS_KEYS);
            String       receipt   = pickDeep(root, RECEIPT_KEYS);

            log.info("[WEBHOOK] id={}, status={}, receipt={}", anyId, statusRaw, receipt);
            log.debug("[WEBHOOK][RAW] {}", raw);

            if (!StringUtils.hasText(anyId)) {
                log.warn("[WEBHOOK] skip: empty id. keys={}", listKeys(root));
                return ResponseEntity.ok().build();
            }

            Optional<PlanInvoiceEntity> optInv = invoiceRepo.findByPiUid(anyId);
            if (optInv.isEmpty()) optInv = findByInvLike(anyId);
            if (optInv.isEmpty()) {
                log.warn("[WEBHOOK] invoice not found by id={}", anyId);
                return ResponseEntity.ok().build();
            }
            PlanInvoiceEntity inv = optInv.get();
            
            // 🛡️ Idempotency / 역순 보호: 이미 PAID면 빠른 종료
            if (inv.getPiStat() == com.dodam.plan.enums.PlanEnums.PiStatus.PAID) {
            	log.info("[WEBHOOK] skip: invoice already PAID (piId={}, anyId={})", inv.getPiId(), anyId);
            	return ResponseEntity.ok().build();
            }

            final String st = normUp(statusRaw);
            if (isPaid(st)) {
                String providerId = null;
                String enrichedJson = raw;
                String payKey = null;

                // (0) orderId -> providerId 시도
                try {
                    JsonNode byOrder = portoneClient.getPaymentByOrderId(anyId);
                    if (byOrder != null && !byOrder.isMissingNode()) {
                        providerId = pickProviderIdByOrderId(byOrder, anyId);
                        if (providerId != null) {
                            JsonNode exact = findItemByOrderId(byOrder, anyId);
                            String rcp = pickDeep(exact, RECEIPT_KEYS);
                            if (!StringUtils.hasText(receipt) && StringUtils.hasText(rcp)) receipt = rcp;
                            payKey = firstNonBlank(pickDeep(exact, setOf("billingKey","billing_key","payKey")));
                        }
                    }
                } catch (Exception e) {
                    log.debug("[WEBHOOK] orderId enrich fail: {}", e.toString());
                }

                // (1) providerId 있으면 정조회
                if (StringUtils.hasText(providerId) && !providerId.startsWith("inv")) {
                    try {
                        var lr = pgSvc.lookup(providerId);
                        if (StringUtils.hasText(lr.rawJson())) {
                            enrichedJson = lr.rawJson();
                            String rcp = tryReceipt(enrichedJson);
                            if (!StringUtils.hasText(receipt) && rcp != null) receipt = rcp;
                            if (!StringUtils.hasText(payKey)) {
                                payKey = safePick(enrichedJson, "billingKey","billing_key","payKey");
                            }
                            CardMeta cm = parseCardMeta(enrichedJson);
                            if (StringUtils.hasText(payKey)) {
                                persistCardMetaByKey(payKey, cm.bin, cm.brand, cm.last4, cm.pg);
                            }
                        }
                    } catch (Exception e) {
                        log.debug("[WEBHOOK] provider lookup fail: {}", e.toString());
                    }
                } else {
                    // (2) 안전 조회
                    var look = pgSvc.safeLookup(anyId);
                    if (StringUtils.hasText(look.rawJson())) {
                        enrichedJson = look.rawJson();
                        String rcp = tryReceipt(enrichedJson);
                        if (!StringUtils.hasText(receipt) && rcp != null) receipt = rcp;
                        if (!StringUtils.hasText(providerId) && StringUtils.hasText(look.paymentId())
                                && !look.paymentId().startsWith("inv")) {
                            providerId = look.paymentId();
                        }
                        if (!StringUtils.hasText(payKey)) {
                            payKey = safePick(enrichedJson, "billingKey","billing_key","payKey");
                        }
                        CardMeta cm = parseCardMeta(enrichedJson);
                        if (StringUtils.hasText(payKey)) {
                            persistCardMetaByKey(payKey, cm.bin, cm.brand, cm.last4, cm.pg);
                        }
                    }
                }

                final String uid = StringUtils.hasText(providerId) ? providerId : anyId;
                invoiceRepo.markPaidAndSetUidIfEmpty(inv.getPiId(), uid, LocalDateTime.now());

                billingSvc.recordAttempt(inv.getPiId(), true, null, uid, firstNonBlank(receipt), enrichedJson);
                log.info("[DEBUG] recordAttempt.enrichedJson={}", enrichedJson);
                return ResponseEntity.ok().build();
            }

            if (isFailed(st)) {
                billingSvc.recordAttempt(inv.getPiId(), false, "WEBHOOK:" + st, anyId, firstNonBlank(receipt), raw);
                return ResponseEntity.ok().build();
            }

            // 불명확하면 조회로 판정
            var look = pgSvc.safeLookup(anyId);
            final String lst = normUp(look.status());
            if (isPaid(lst)) {
                String rcp = firstNonBlank(tryReceipt(look.rawJson()), receipt);
                String payKey = safePick(look.rawJson(), "billingKey","billing_key","payKey");
                CardMeta cm = parseCardMeta(look.rawJson());
                if (StringUtils.hasText(payKey)) {
                    persistCardMetaByKey(payKey, cm.bin, cm.brand, cm.last4, cm.pg);
                }
                String uid = StringUtils.hasText(look.paymentId()) ? look.paymentId() : anyId;
                invoiceRepo.markPaidAndSetUidIfEmpty(inv.getPiId(), uid, LocalDateTime.now());
                billingSvc.recordAttempt(inv.getPiId(), true, null, uid, rcp, look.rawJson());
            } else if (isFailed(lst)) {
                billingSvc.recordAttempt(inv.getPiId(), false, "LOOKUP:" + lst, anyId, firstNonBlank(receipt), look.rawJson());
            } else {
                billingSvc.recordAttempt(inv.getPiId(), false, "LOOKUP:PENDING", anyId, firstNonBlank(receipt), look.rawJson());
            }
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("[WEBHOOK] error: {}", e.toString(), e);
            return ResponseEntity.ok().build();
        }
    }

    /* ================= helpers ================= */

    private static Set<String> setOf(String... a){ return new HashSet<>(Arrays.asList(a)); }
    private static String normUp(String s){ return s==null ? null : s.trim().toUpperCase(Locale.ROOT); }
    private static boolean isPaid(String s){
        String u = normUp(s);
        return "PAID".equals(u) || "SUCCEEDED".equals(u) || "SUCCESS".equals(u) || "PARTIAL_PAID".equals(u);
    }
    private static boolean isFailed(String s){
        String u = normUp(s);
        return "FAILED".equals(u) || "CANCELED".equals(u) || "CANCELLED".equals(u);
    }

    private Optional<PlanInvoiceEntity> findByInvLike(String anyId){
        try {
            Long invId = Long.parseLong(anyId.replaceFirst("^inv","").split("-")[0].replaceAll("[^0-9]",""));
            return invoiceRepo.findById(invId);
        } catch (Exception ignore) { return Optional.empty(); }
    }

    private static String pickDeep(JsonNode root, Set<String> keys) {
        if (root == null) return null;
        for (String k : keys) {
            String v = findByPath(root, k);
            if (StringUtils.hasText(v)) return v;
        }
        return findByKeyAnywhere(root, keys);
    }
    private static String findByPath(JsonNode root, String dotted) {
        String[] parts = dotted.split("\\.");
        JsonNode cur = root;
        for (String p : parts) {
            if (cur == null) return null;
            cur = cur.get(p);
        }
        if (cur == null || cur.isMissingNode() || cur.isNull()) return null;
        return cur.isValueNode() ? cur.asText(null) : null;
    }
    private static String norm(String k){ return k==null? null : k.replace("_","").replace(".","").toLowerCase(Locale.ROOT); }
    private static String findByKeyAnywhere(JsonNode n, Set<String> keys){
        Set<String> norms = new HashSet<>();
        for (String k : keys) norms.add(norm(k));
        return dfs(n, norms);
    }
    private static String dfs(JsonNode n, Set<String> want){
        if (n == null) return null;
        if (n.isObject()) {
            var it = n.fields();
            while (it.hasNext()){
                var e = it.next();
                String k = norm(e.getKey());
                JsonNode v = e.getValue();
                if (want.contains(k) && v.isValueNode()){
                    String s = v.asText(null);
                    if (StringUtils.hasText(s)) return s;
                }
                String deep = dfs(v, want);
                if (StringUtils.hasText(deep)) return deep;
            }
        } else if (n.isArray()){
            for (JsonNode c : n){
                String deep = dfs(c, want);
                if (StringUtils.hasText(deep)) return deep;
            }
        }
        return null;
    }

    private static String firstNonBlank(String... v){ if (v==null) return null; for (String s : v) if (StringUtils.hasText(s)) return s; return null; }

    private static String tryReceipt(String rawJson){
        try {
            JsonNode r = M.readTree(rawJson);
            String r1 = pickDeep(r, RECEIPT_KEYS);
            if (StringUtils.hasText(r1)) return r1;
            JsonNode items = r.get("items");
            if (items != null && items.isArray() && items.size() > 0){
                return pickDeep(items.get(0), RECEIPT_KEYS);
            }
        } catch (Exception ignore) {}
        return null;
    }

    private static JsonNode findItemByOrderId(JsonNode root, String orderId) {
        if (root == null || !StringUtils.hasText(orderId)) return null;
        if (orderId.equals(root.path("orderId").asText(null))) return root;

        JsonNode items = root.get("items");
        if (items != null && items.isArray()) {
            for (JsonNode it : items) {
                if (orderId.equals(it.path("orderId").asText(null))) return it;
            }
        }
        return null;
    }
    private static String pickProviderIdByOrderId(JsonNode root, String orderId) {
        JsonNode n = findItemByOrderId(root, orderId);
        if (n == null) return null;
        return pickDeep(n, setOf("id","payment.id","transactionUid","transaction_uid","tx_id"));
    }
    private static String safePick(String rawJson, String... keys) {
        try {
            return pickDeep(M.readTree(rawJson), setOf(keys));
        } catch (Exception e) {
            return null;
        }
    }

    /* ===== 카드 메타 파싱/저장 ===== */
    private record CardMeta(String bin, String brand, String last4, String pg) {}
    private static CardMeta parseCardMeta(String rawJson) {
        try {
            JsonNode r = M.readTree(rawJson);
            String bin   = pickDeep(r, setOf("card.bin","methodDetail.card.bin","method.card.bin"));
            String brand = pickDeep(r, setOf("card.company","card.brand","methodDetail.brand","card.issuer","card.acquirer"));
            String last4 = pickDeep(r, setOf("card.lastFourDigits","card.last4","methodDetail.card.last4"));
            if (!StringUtils.hasText(last4)) {
                String masked = pickDeep(r, setOf("card.number","card.cardNumber"));
                if (StringUtils.hasText(masked) && masked.length() >= 4) {
                    last4 = masked.substring(masked.length() - 4);
                }
            }
            String pg = pickDeep(r, setOf("pgProvider","gateway","pg","provider"));
            return new CardMeta(bin, brand, last4, pg);
        } catch (Exception e) {
            return new CardMeta(null,null,null,null);
        }
    }

    private boolean persistCardMetaByKey(String payKey, String bin, String brand, String last4, String pg) {
        if (!StringUtils.hasText(payKey)) return false;
        return paymentRepo.findByPayKey(payKey).map(p -> {
            boolean changed = false;
            String dLast4 = sanitizeLast4(last4);
            if (StringUtils.hasText(bin)   && !bin.equals(p.getPayBin()))     { p.setPayBin(bin);       changed = true; }
            if (StringUtils.hasText(brand) && !brand.equals(p.getPayBrand())) { p.setPayBrand(brand);   changed = true; }
            if (StringUtils.hasText(dLast4)&& !dLast4.equals(p.getPayLast4())){ p.setPayLast4(dLast4);  changed = true; }
            if (StringUtils.hasText(pg)    && !pg.equals(p.getPayPg()))       { p.setPayPg(pg);         changed = true; }
            if (changed) paymentRepo.save(p);
            return changed;
        }).orElse(false);
    }
    private static String sanitizeLast4(String v) {
        if (!StringUtils.hasText(v)) return null;
        String digits = v.replaceAll("\\D", "");
        if (digits.length() >= 4) return digits.substring(digits.length() - 4);
        return null;
    }

    private static List<String> listKeys(JsonNode n){
        List<String> out = new ArrayList<>();
        collectKeys(n, out, "");
        return out;
    }
    private static void collectKeys(JsonNode n, List<String> out, String prefix){
        if (n == null) return;
        if (n.isObject()){
            var it = n.fields();
            while (it.hasNext()){
                var e = it.next();
                String key = prefix.isEmpty() ? e.getKey() : prefix + "." + e.getKey();
                out.add(key);
                collectKeys(e.getValue(), out, key);
            }
        } else if (n.isArray()){
            int i=0;
            for (JsonNode c : n){
                collectKeys(c, out, prefix + "["+(i++)+"]");
            }
        }
    }
}
