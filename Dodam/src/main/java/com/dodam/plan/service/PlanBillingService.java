package com.dodam.plan.service;

import com.dodam.plan.Entity.PlanAttemptEntity;
import com.dodam.plan.Entity.PlanInvoiceEntity;
import com.dodam.plan.Entity.PlanMember;
import com.dodam.plan.Entity.PlanPaymentEntity;
import com.dodam.plan.dto.PlanLookupResult;
import com.dodam.plan.enums.PlanEnums.PattResult;
import com.dodam.plan.repository.PlanAttemptRepository;
import com.dodam.plan.repository.PlanInvoiceRepository;
import com.dodam.plan.repository.PlanPaymentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

@Slf4j
@Service
@Transactional
@AllArgsConstructor
public class PlanBillingService {

    private final PlanAttemptRepository attemptRepo;
    private final PlanInvoiceRepository invoiceRepo;
    private final PlanPaymentRepository paymentRepo;
    private final PlanPortoneClientService pgSvc;

    private static final ObjectMapper OM = new ObjectMapper();
    private static final int MAX_JSON_LEN = 4000;
    private static final int MAX_URL_LEN = 1024;

    /** 기존 5-파라미터 호출도 그대로 받도록 유지(호환) */
    public void recordAttempt(Long invoiceId, boolean isSuccess, String respUid, String receiptUrl, String respJson) {
        recordAttempt(invoiceId, isSuccess, null, respUid, receiptUrl, respJson);
    }

    /**
     * 통합 기록 메서드(컨트롤러/웹훅/잡 공용)
     */
    public void recordAttempt(Long invoiceId,
                              boolean isSuccess,
                              String statusReason,
                              String respUid,
                              String receiptUrl,
                              String respJson) {

        Objects.requireNonNull(invoiceId, "invoiceId is required");

        PlanInvoiceEntity inv = invoiceRepo.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("invoice not found: " + invoiceId));

        // 1) Attempt 저장
        PlanAttemptEntity att = new PlanAttemptEntity();
        att.setInvoice(inv);
        att.setPattResult(resolveResult(isSuccess, statusReason));
        if (StringUtils.hasText(statusReason)) {
            att.setPattFail(statusReason);
        }
        if (StringUtils.hasText(respUid)) {
            att.setPattUid(respUid.trim());
        }
        if (StringUtils.hasText(receiptUrl)) {
            att.setPattUrl(cut(receiptUrl.trim(), MAX_URL_LEN));
        }

        String safeJson = sanitizeRespJson(respJson);
        if (StringUtils.hasText(safeJson)) {
            att.setPattResponse(cut(safeJson, MAX_JSON_LEN));
        }
        attemptRepo.save(att);

        // 2) 카드 메타/영수증 보강 업데이트
        try {
            boolean successLike = isPaidLike(safeJson) || isSuccess;
            if (!successLike) {
                log.info("[Billing] skip card-meta update (not success-like): inv={}, flag={}, jsonPaid={}",
                        invoiceId, isSuccess, isPaidLike(safeJson));
                return;
            }

            // 대상 결제수단 찾기
            PlanPaymentEntity pay = resolvePaymentEntity(inv, safeJson, respUid);
            if (pay == null) {
                log.warn("[Billing] no payment bound (invoiceId={}), skip card-meta update", invoiceId);
                return;
            }

            // JSON에서 카드 파편 추출
            CardPieces pieces = extractCardPiecesFromJson(safeJson);

            // 부족하면 상세 룩업으로 보강
            PlanLookupResult looked = null;
            if (pieces.isIncomplete() && StringUtils.hasText(respUid)) {
                PlanPortoneClientService.LookupResponse lr = pgSvc.safeLookup(respUid);
                if (lr != null && StringUtils.hasText(lr.raw())) {
                    // ✅ record 시그니처에 맞춰 success 플래그 포함
                    looked = new PlanLookupResult(true, lr.id(), lr.status(), lr.raw());
                    CardPieces sub = extractCardPiecesFromJson(looked.rawJson());
                    pieces = pieces.merge(sub);
                }
            }

            // 영수증 URL 보강
            if (!StringUtils.hasText(att.getPattUrl()) && looked != null && StringUtils.hasText(looked.rawJson())) {
                String tryUrl = resolveReceiptUrl(null, looked.rawJson(), respUid);
                if (StringUtils.hasText(tryUrl)) {
                    att.setPattUrl(cut(tryUrl, MAX_URL_LEN));
                    attemptRepo.save(att);
                }
            }

            boolean changed = false;

            if (StringUtils.hasText(pieces.bin) && !pieces.bin.equals(pay.getPayBin())) {
                pay.setPayBin(pieces.bin); changed = true;
            }

            String brandKo = firstNonBlank(
                    pieces.brandKo,
                    normalizeIssuerKo(pieces.issuerKo),
                    normalizeIssuerKo(pieces.companyKo),
                    fromMaskedNameAsIssuerKo(pieces.maskedName)
            );
            if (StringUtils.hasText(brandKo) && !brandKo.equals(pay.getPayBrand())) {
                pay.setPayBrand(brandKo); changed = true;
            }

            String last4 = tail4Digits(firstNonBlank(pieces.last4, pieces.maskedNumber));
            if (StringUtils.hasText(last4) && !last4.equals(pay.getPayLast4())) {
                pay.setPayLast4(last4); changed = true;
            }

            if (!"TossPayments".equals(pay.getPayPg())) {
                pay.setPayPg("TossPayments"); changed = true;
            }

            if (!StringUtils.hasText(pay.getPayRaw()) && StringUtils.hasText(safeJson)) {
                pay.setPayRaw(cut(safeJson, MAX_JSON_LEN)); changed = true;
            }

            if (changed) {
                paymentRepo.save(pay);
                log.info("[Billing] card meta updated → payId={}, brand={}, last4={}, bin={}",
                        pay.getPayId(), pay.getPayBrand(), pay.getPayLast4(), pay.getPayBin());
            } else {
                log.info("[Billing] card meta unchanged → payId={}", pay.getPayId());
            }

        } catch (Exception e) {
            log.error("[Billing] card meta update failed → {}", e.getMessage(), e);
        }
    }

    /* ========================= 내부 유틸 ========================= */

    private PattResult resolveResult(boolean isSuccess, String reason) {
        if (isSuccess) return PattResult.SUCCESS;
        String up = reason == null ? "" : reason.trim().toUpperCase(Locale.ROOT);
        if (up.contains("FAIL") || up.contains("ERROR") || up.contains("CANCEL")) return PattResult.FAIL;
        return PattResult.PENDING;
    }

    private String sanitizeRespJson(String respJson) {
        if (!StringUtils.hasText(respJson)) return null;
        String trimmed = respJson.trim();
        if (trimmed.contains("\"error\":\"no paymentId\"")) return null;
        return trimmed;
    }

    private PlanPaymentEntity resolvePaymentEntity(PlanInvoiceEntity inv, String json, String respUid) {
        PlanMember pm = inv.getPlanMember();
        if (pm != null && pm.getPayment() != null) {
            return pm.getPayment();
        }
        String bk = extractBillingKey(json);
        if (!StringUtils.hasText(bk) && StringUtils.hasText(respUid)) {
            PlanPortoneClientService.LookupResponse lr = pgSvc.safeLookup(respUid);
            if (lr != null && StringUtils.hasText(lr.raw())) {
                PlanLookupResult look = new PlanLookupResult(true, lr.id(), lr.status(), lr.raw());
                if (StringUtils.hasText(look.rawJson())) {
                    bk = extractBillingKey(look.rawJson());
                }
            }
        }
        if (StringUtils.hasText(bk)) {
            return paymentRepo.findByPayKey(bk).orElse(null);
        }
        return null;
    }

    private boolean isPaidLike(String json) {
        if (!StringUtils.hasText(json)) return false;
        String upper = json.toUpperCase(Locale.ROOT);
        return upper.contains("\"STATUS\":\"PAID\"")
                || upper.contains("\"PAYMENTSTATUS\":\"PAID\"")
                || upper.contains("\"STATUS\":\"DONE\"")
                || upper.contains("\"SUCCEEDED\"");
    }

    private String cut(String s, int max) {
        if (!StringUtils.hasText(s)) return s;
        return (s.length() <= max) ? s : s.substring(0, max);
    }

    /** 영수증 URL 추출 */
    private String resolveReceiptUrl(String given, String json, String respUid) {
        if (StringUtils.hasText(given)) return given;
        try {
            if (StringUtils.hasText(json)) {
                JsonNode root = OM.readTree(json);
                String v = asText(root, "receiptUrl");
                if (StringUtils.hasText(v)) return v;
                v = asText(root, "pgResponse.receipt.url");
                if (StringUtils.hasText(v)) return v;
                JsonNode items = root.path("items");
                if (items.isArray() && items.size() > 0) {
                    v = asText(items.get(0), "receiptUrl");
                    if (StringUtils.hasText(v)) return v;
                    v = asText(items.get(0), "pgResponse.receipt.url");
                    if (StringUtils.hasText(v)) return v;
                }
            }
        } catch (Exception ignore) {}
        return null;
    }

    private String extractBillingKey(String json) {
        if (!StringUtils.hasText(json)) return null;
        try {
            JsonNode root = OM.readTree(json);
            String v = asText(root, "billingKey");
            if (StringUtils.hasText(v)) return v;
            JsonNode items = root.path("items");
            if (items.isArray() && items.size() > 0) {
                v = asText(items.get(0), "billingKey");
                if (StringUtils.hasText(v)) return v;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private CardPieces extractCardPiecesFromJson(String json) {
        CardPieces p = new CardPieces();
        if (!StringUtils.hasText(json)) return p;
        try {
            JsonNode root = OM.readTree(json);

            JsonNode method = root.path("method");
            if (!method.isMissingNode() && "PaymentMethodCard".equals(asText(method, "type"))) {
                JsonNode card = method.path("card");
                if (!card.isMissingNode()) {
                    p.publisher    = asText(card, "publisher");
                    p.issuerKo     = asText(card, "issuer");
                    p.brandEn      = asText(card, "brand");
                    p.type         = asText(card, "type");
                    p.ownerType    = asText(card, "ownerType");
                    p.bin          = asText(card, "bin");
                    p.maskedName   = asText(card, "name");
                    p.maskedNumber = asText(card, "number");
                }
            }

            JsonNode pgResponse = root.path("pgResponse");
            if (!pgResponse.isMissingNode()) {
                JsonNode card = pgResponse.path("card");
                if (!card.isMissingNode()) {
                    String company = asText(card, "company");
                    if (StringUtils.hasText(company)) p.companyKo = company;
                    String num = asText(card, "number");
                    if (StringUtils.hasText(num)) {
                        if (!StringUtils.hasText(p.maskedNumber)) p.maskedNumber = num;
                        if (!StringUtils.hasText(p.last4)) p.last4 = tail4Digits(num);
                    }
                    String issuerCode = asText(card, "issuerCode");
                    if (StringUtils.hasText(issuerCode) && !StringUtils.hasText(p.issuerCode)) {
                        p.issuerCode = normalizeIssuerCode(issuerCode);
                    }
                }
            }

            if (!StringUtils.hasText(p.last4)) {
                p.last4 = tail4Digits(p.maskedNumber);
            }
            if (!StringUtils.hasText(p.brandKo)) {
                String ko = firstNonBlank(p.companyKo, fromMaskedNameAsIssuerKo(p.maskedName), normalizeIssuerKo(p.issuerKo));
                p.brandKo = ko;
            }
        } catch (Exception e) {
            log.warn("[Billing] extractCardPiecesFromJson failed: {}", e.getMessage());
        }
        return p;
    }

    private static String firstNonBlank(String... arr) {
        if (arr == null) return null;
        for (String s : arr) if (StringUtils.hasText(s)) return s;
        return null;
    }

    private static String asText(JsonNode node, String dottedPath) {
        if (node == null || node.isMissingNode() || !StringUtils.hasText(dottedPath)) return null;
        String[] parts = dottedPath.split("\\.");
        JsonNode cur = node;
        for (String p : parts) {
            cur = cur.path(p);
            if (cur.isMissingNode()) return null;
        }
        if (cur.isTextual()) return cur.asText();
        if (cur.isNumber()) return cur.asText();
        return null;
    }

    public static String tail4Digits(String cardNumberLike) {
        if (!StringUtils.hasText(cardNumberLike)) return null;
        String digits = cardNumberLike.replaceAll("[^0-9]", "");
        if (digits.length() < 4) return null;
        return digits.substring(digits.length() - 4);
    }

    private static String fromMaskedNameAsIssuerKo(String maskedName) {
        if (!StringUtils.hasText(maskedName)) return null;
        String s = maskedName.trim();
        if (s.endsWith("카드")) return s.substring(0, s.length() - 2).trim();
        return s;
    }

    public static String normalizeIssuerKo(String issuerLike) {
        if (!StringUtils.hasText(issuerLike)) return null;
        String u = issuerLike.trim().toUpperCase(Locale.ROOT);
        Map<String, String> map = new HashMap<>();
        map.put("HYUNDAI_CARD", "현대");
        map.put("SHINHAN_CARD", "신한");
        map.put("KOOKMIN_CARD", "국민");
        map.put("HANA_CARD", "하나");
        map.put("SAMSUNG_CARD", "삼성");
        map.put("LOTTES_CARD", "롯데");
        map.put("KAKAO_BANK", "카카오뱅크");
        map.put("TOSS_BANK", "토스뱅크");
        return map.getOrDefault(u, issuerLike);
    }

    public static String normalizeIssuerCode(String codeLike) {
        if (!StringUtils.hasText(codeLike)) return null;
        String onlyDigits = codeLike.replaceAll("[^0-9]", "");
        return onlyDigits.isEmpty() ? null : onlyDigits;
    }

    @Getter
    @NoArgsConstructor
    private static class CardPieces {
        String publisher;
        String issuerKo;
        String issuerCode;
        String companyKo;
        String brandEn;
        String brandKo;
        String type;
        String ownerType;
        String bin;
        String maskedName;
        String maskedNumber;
        String last4;

        boolean isIncomplete() {
            return !StringUtils.hasText(bin) || !StringUtils.hasText(brandKo) || !StringUtils.hasText(last4);
        }

        CardPieces merge(CardPieces other) {
            if (other == null) return this;
            CardPieces r = new CardPieces();
            r.publisher    = firstNonBlank(this.publisher, other.publisher);
            r.issuerKo     = firstNonBlank(this.issuerKo, other.issuerKo);
            r.issuerCode   = firstNonBlank(this.issuerCode, other.issuerCode);
            r.companyKo    = firstNonBlank(this.companyKo, other.companyKo);
            r.brandEn      = firstNonBlank(this.brandEn, other.brandEn);
            r.brandKo      = firstNonBlank(this.brandKo, other.brandKo);
            r.type         = firstNonBlank(this.type, other.type);
            r.ownerType    = firstNonBlank(this.ownerType, other.ownerType);
            r.bin          = firstNonBlank(this.bin, other.bin);
            r.maskedName   = firstNonBlank(this.maskedName, other.maskedName);
            r.maskedNumber = firstNonBlank(this.maskedNumber, other.maskedNumber);
            r.last4        = firstNonBlank(this.last4, other.last4);
            return r;
        }
    }
}
