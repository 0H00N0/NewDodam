package com.dodam.plan.support;

import com.dodam.plan.dto.PlanCardMeta;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class PlanPortoneJsonExtractor {
    private PlanPortoneJsonExtractor() {}

    /** 결제 응답 JSON에서 영수증/상세 URL 후보를 탐색, 없으면 관리자 상세 URL로 폴백 */
    public static String extractReceiptUrl(ObjectMapper mapper, String rawJson, String paymentId) {
        try {
            JsonNode root = mapper.readTree(rawJson);
            String v = pickText(root,
                    "/receiptUrl",
                    "/receipt/url",
                    "/urls/receipt",
                    "/url",
                    "/paymentUrl",
                    "/checkout/url",
                    "/transactions/0/receiptUrl",
                    "/transactions/0/url");
            if (v != null && !v.isBlank()) return v;
        } catch (Exception ignore) {}
        return "https://admin.portone.io/payments/" + paymentId;
    }

    /** “이번 결제 응답”에서만 카드 메타 추출 (프로필/이전 결제 금지) */
    public static PlanCardMeta extractCardMeta(ObjectMapper mapper, String rawJson) {
        PlanCardMeta m = new PlanCardMeta();
        try {
            JsonNode root = mapper.readTree(rawJson);
            JsonNode card = firstNonNull(
                    root.at("/method/card"),
                    root.at("/card"),
                    root.at("/paymentMethod/card"),
                    root.at("/payload/card"),
                    root.at("/transactions/0/card")
            );

            String bin   = firstNonBlank(pickText(card, "/bin"), pickText(root, "/bin"), pickText(root, "/cardBin"));
            String last4 = firstNonBlank(pickText(card, "/last4"), pickText(card, "/number/last4"),
                                         pickText(root, "/last4"), pickText(root, "/cardNumberSuffix"));
            String brand = firstNonBlank(pickText(card, "/company"), pickText(card, "/brand"),
                                         pickText(root, "/cardCompany"), pickText(root, "/cardBrand"));
            String pg    = firstNonBlank(pickText(root, "/pgProvider"), pickText(root, "/pgCode"),
                                         pickText(root, "/channel/pgProvider"), pickText(root, "/provider"),
                                         pickText(root, "/pg"));

            if (bin   != null) m.setBin(bin);
            if (last4 != null) m.setLast4(last4);
            if (brand != null) m.setBrand(brand);
            if (pg    != null) m.setPg(pg);

        } catch (Exception ignore) {}
        return m;
    }

    /* ---------- helpers ---------- */
    private static String pickText(JsonNode n, String... ptrs) {
        if (n == null || n.isMissingNode()) return null;
        for (String p : ptrs) {
            JsonNode v = n.at(p);
            if (v != null && !v.isMissingNode() && v.isTextual()) {
                String s = v.asText();
                if (s != null && !s.isBlank()) return s;
            }
        }
        return null;
    }
    private static JsonNode firstNonNull(JsonNode... nodes) {
        for (JsonNode n : nodes) {
            if (n != null && !n.isMissingNode() && !n.isNull()) return n;
        }
        return null;
    }
    private static String firstNonBlank(String... vals) {
        if (vals == null) return null;
        for (String v : vals) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }
}
