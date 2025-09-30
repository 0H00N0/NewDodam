package com.dodam.main.repository;

import com.dodam.product.entity.ProductEntity;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

public final class MainProductSearchSpecs {
    private MainProductSearchSpecs() {}

    private static String likePattern(String raw) {
        if (raw == null) return null;
        String s = raw.trim().toLowerCase(Locale.ROOT);
        s = s.replace("%", "\\%").replace("_", "\\_");
        return "%" + s + "%";
    }

    /** 상품명(proname) LIKE 만 사용 */
    public static Specification<ProductEntity> nameContains(String q) {
        if (q == null || q.isBlank()) return null;
        final String like = likePattern(q);
        return (root, query, cb) ->
                cb.like(cb.lower(root.<String>get("proname")), like);
    }
}
