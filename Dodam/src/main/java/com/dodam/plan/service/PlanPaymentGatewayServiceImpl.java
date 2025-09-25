// src/main/java/com/dodam/plan/service/PlanPaymentGatewayServiceImpl.java
package com.dodam.plan.service;

import com.dodam.plan.config.PlanPortoneProperties;
import com.dodam.plan.dto.PlanCardMeta;
import com.dodam.plan.dto.PlanLookupResult;
import com.dodam.plan.dto.PlanPayResult;
import com.dodam.plan.dto.PlanPaymentLookupResult;
import com.dodam.plan.repository.PlanAttemptRepository;
import com.dodam.plan.service.PlanPortoneClientService.ConfirmRequest;
import com.dodam.plan.service.PlanPortoneClientService.ConfirmResponse;
import com.dodam.plan.service.PlanPortoneClientService.LookupResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Slf4j
@Service
public class PlanPaymentGatewayServiceImpl implements PlanPaymentGatewayService {

    private final PlanPortoneClientService portone;
    private final PlanPortoneProperties props;
    private final ObjectMapper mapper = new ObjectMapper();
    private final PlanAttemptRepository attemptRepo;

    public PlanPaymentGatewayServiceImpl(
            @Qualifier("planPortoneClientServiceImpl") PlanPortoneClientService portone,
            PlanPortoneProperties props,
            PlanAttemptRepository attemptRepo
    ) {
        this.portone = portone;
        this.props = props;
        this.attemptRepo = attemptRepo;
    }

    @Override
    public PlanPayResult payByBillingKey(String paymentId, String billingKey, long amount, String customerId) {
        return payByBillingKey(
                paymentId,
                billingKey,
                amount,
                props.getCurrency() != null ? props.getCurrency() : "KRW",
                "Dodam Subscription",
                props.getStoreId(),
                customerId,
                props.getChannelKey()
        );
    }

    @Override
    public PlanPayResult payByBillingKey(
            String paymentId,
            String billingKey,
            long amount,
            String currency,
            String orderName,
            String storeId,
            String customerId,
            String channelKey
    ) {
        // 1) confirm 요청
        ConfirmRequest req = new ConfirmRequest(
                paymentId, billingKey, amount, currency, customerId, orderName,
                Boolean.TRUE.equals(props.getIsTest())
        );
        ConfirmResponse res = portone.confirmByBillingKey(req);

        String status = norm(res.status());
        boolean success = isPaidStatus(status);

        String providerPaymentUid = n(res.id());
        String receiptUrl = null;

        // 최초 confirm raw (간이 JSON일 수 있음)
        String rawToStore = res.raw();

        // 확인 가능한 범위에서 영수증 URL 스니핑
        try {
            if (res.raw() != null && res.raw().startsWith("{")) {
                JsonNode root = mapper.readTree(res.raw());
                receiptUrl = n(root.path("receiptUrl").asText(null));
                if (receiptUrl == null) receiptUrl = n(root.path("receipt").path("url").asText(null));
                if (receiptUrl == null) receiptUrl = n(root.at("/urls/receipt").asText(null));
                if (receiptUrl == null) receiptUrl = n(root.at("/transactions/0/receiptUrl").asText(null));
            }
        } catch (Exception ignore) {}

        // 우리 쪽에 저장할 uid (provider 가 있으면 그걸 우선)
        String payUidToStore = n(providerPaymentUid) != null ? providerPaymentUid : paymentId;

        // 2) 보강 폴링 (최대 6초)
        if (!success) {
            final String loopKey = resolvePaymentId(firstNonBlank(providerPaymentUid, paymentId));
            final long until = System.currentTimeMillis() + 6_000L;

            while (System.currentTimeMillis() < until) {
                try {
                    LookupResponse lr = portone.lookupPayment(loopKey);
                    String st = norm(lr.status());

                    if (isPaidStatus(st)) {
                        success = true;
                        status = st;
                        providerPaymentUid = n(lr.id());
                        payUidToStore = n(providerPaymentUid) != null ? providerPaymentUid : paymentId;

                        // ✅ 최종 원문 교체: 폴링으로 받은 상세 raw를 저장
                        if (lr.raw() != null && !lr.raw().isBlank()) {
                            rawToStore = lr.raw();
                            try {
                                JsonNode root = mapper.readTree(lr.raw());
                                String rcp = n(root.path("receiptUrl").asText(null));
                                if (rcp == null) rcp = n(root.path("receipt").path("url").asText(null));
                                if (rcp != null) receiptUrl = rcp;
                            } catch (Exception ignore) {}
                        }
                        break;
                    }

                    if (isFailedStatus(st)) {
                        status = st;
                        break;
                    }
                } catch (Exception ignore) { /* swallow */ }

                try { Thread.sleep(700); }
                catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
            }
        }

        // 3) ✅ DB는 여기서 건드리지 않고 결과만 반환
        //    success 시 failReason=null, status에는 최종 상태(예: PAID/PENDING/FAILED...), raw는 rawToStore 반환
        return new PlanPayResult(success, payUidToStore, receiptUrl, success ? null : status, status, rawToStore);
    }

    private String resolvePaymentId(String anyId) {
        if (!StringUtils.hasText(anyId)) return anyId;
        // inv{invoiceId}-ts... 형태면 최신 attempt 에서 provider uid 가져와 조회 안정화
        if (anyId.startsWith("inv")) {
            Long invoiceId = extractInvoiceId(anyId);
            return attemptRepo.findLatestPaymentUidByInvoiceId(invoiceId)
                    .filter(StringUtils::hasText)
                    .orElse(null);
        }
        return anyId;
    }

    @Override
    public PlanLookupResult safeLookup(String paymentId) {
        try {
            String pid = resolvePaymentId(paymentId);
            if (!StringUtils.hasText(pid)) {
                return new PlanLookupResult(false, paymentId, "NOT_FOUND", "{\"error\":\"no providerId for invoice\"}");
            }
            LookupResponse r = portone.lookupPayment(pid);
            boolean ok = isPaidStatus(r.status());
            return new PlanLookupResult(ok, r.id(), r.status(), r.raw());
        } catch (Exception e) {
            return new PlanLookupResult(false, paymentId, "ERROR", "{\"error\":\"" + e + "\"}");
        }
    }

    @Override
    public PlanLookupResult lookup(String paymentId) {
        String pid = resolvePaymentId(paymentId);
        if (!StringUtils.hasText(pid)) {
            return new PlanLookupResult(false, paymentId, "NOT_FOUND", "{\"error\":\"no providerId for invoice\"}");
        }
        var r = portone.lookupPayment(pid);
        boolean ok = isPaidStatus(r.status());
        return new PlanLookupResult(ok, r.id(), r.status(), r.raw());
    }

    @Override
    public PlanPaymentLookupResult lookupPayment(String paymentId) {
        String pid = resolvePaymentId(paymentId);
        try {
            if (!StringUtils.hasText(pid)) {
                return new PlanPaymentLookupResult(paymentId, "NOT_FOUND", "{\"error\":\"no providerId for invoice\"}", HttpStatus.OK);
            }
            var r = portone.lookupPayment(pid);
            return new PlanPaymentLookupResult(r.id(), r.status(), r.raw(), HttpStatus.OK);
        } catch (Exception e) {
            return new PlanPaymentLookupResult(paymentId, "ERROR", e.toString(), HttpStatus.BAD_GATEWAY);
        }
    }

    // -------- 카드 메타 추출 --------
    @Override
    public PlanCardMeta extractCardMeta(String rawJson) {
        if (!StringUtils.hasText(rawJson)) {
            return new PlanCardMeta(null, null, null, null, null, false, null);
        }
        try {
            JsonNode root = mapper.readTree(rawJson);

            String billingKey = firstNonBlank(
                    get(root, "billingKey"),
                    get(root, "method", "billingKey"),
                    get(root, "method", "card", "billingKey"),
                    get(root, "billing", "billingKey"),
                    get(root, "payment", "method", "billingKey")
            );

            String bin   = firstNonBlank(get(root, "method", "card", "bin"),   get(root, "card", "bin"));
            String brand = firstNonBlank(get(root, "method", "card", "brand"), get(root, "card", "brand"));
            String last4 = firstNonBlank(
                    get(root, "method", "card", "last4"),
                    last4FromMasked(get(root, "method", "card", "number")),
                    last4FromMasked(get(root, "card", "number"))
            );
            String pg = firstNonBlank(
                    get(root, "pgProvider"), get(root, "pgCompany"),
                    get(root, "payment", "pgProvider"), get(root, "payment", "pgCompany")
            );

            if (log.isDebugEnabled()) {
                log.debug("[extractCardMeta] billingKey={}, bin={}, brand={}, last4={}, pg={}",
                        billingKey, bin, brand, last4, pg);
            }

            return new PlanCardMeta(billingKey, brand, bin, last4, pg, false, null);
        } catch (Exception e) {
            log.warn("[extractCardMeta] parse failed: {}", e.toString());
            return new PlanCardMeta(null, null, null, null, null, false, null);
        }
    }

    // ---- 유틸 ----
    private Long extractInvoiceId(String uid) {
        String num = uid.replaceFirst("^inv","").split("-")[0].replaceAll("[^0-9]","");
        return Long.parseLong(num);
    }

    private static String get(JsonNode n, String... path) {
        if (n == null) return null;
        JsonNode cur = n;
        for (String p : path) cur = (cur == null ? null : cur.path(p));
        if (cur == null) return null;
        String v = cur.asText(null);
        return (v != null && !v.isBlank()) ? v : null;
    }
    private static String last4FromMasked(String masked) {
        if (masked == null) return null;
        String digits = masked.replaceAll("\\D", "");
        return digits.length() >= 4 ? digits.substring(digits.length() - 4) : null;
    }
    private static String firstNonBlank(String... arr) {
        if (arr == null) return null;
        for (String s : arr) if (s != null && !s.isBlank()) return s;
        return null;
    }
    private String n(String s) { return (s == null || s.isBlank()) ? null : s; }
    private String norm(String v){ return (v==null) ? "" : v.trim().toUpperCase(Locale.ROOT); }
    private boolean isPaidStatus(String status) {
        String s = norm(status);
        return s.equals("PAID") || s.equals("SUCCEEDED") || s.equals("SUCCESS") || s.equals("PARTIAL_PAID");
    }
    private boolean isFailedStatus(String status) {
        String s = norm(status);
        return s.equals("FAILED") || s.equals("CANCELED") || s.equals("CANCELLED");
    }
}
