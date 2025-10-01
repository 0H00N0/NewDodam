// src/main/java/com/dodam/main/repository/MainProductSearchSpecs.java
package com.dodam.main.repository;

import com.dodam.product.entity.ProductEntity;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Locale;

public final class MainProductSearchSpecs {
    private MainProductSearchSpecs() {}

    private static String likePattern(String raw) {
        if (raw == null) return null;
        String s = raw.trim().toLowerCase(Locale.ROOT);
        s = s.replace("%", "\\%").replace("_", "\\_");
        return "%" + s + "%";
    }

    /** 상품명(proname) LIKE */
    public static Specification<ProductEntity> nameContains(String q) {
        if (q == null || q.isBlank()) return null;
        final String like = likePattern(q);
        return (root, query, cb) -> cb.like(cb.lower(root.get("proname")), like);
    }

    /** 연령대(proage) BETWEEN */
    public static Specification<ProductEntity> ageBetween(Integer minAge, Integer maxAge) {
        if (minAge == null && maxAge == null) return null;
        return (root, query, cb) -> {
            if (minAge != null && maxAge != null) {
                return cb.between(root.get("proage"), minAge, maxAge);
            } else if (minAge != null) {
                return cb.greaterThanOrEqualTo(root.get("proage"), minAge);
            } else {
                return cb.lessThanOrEqualTo(root.get("proage"), maxAge);
            }
        };
    }

    /** 가격대(proborrow) BETWEEN (BigDecimal 기준) */
    public static Specification<ProductEntity> priceBetween(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice == null && maxPrice == null) return null;
        return (root, query, cb) -> {
            if (minPrice != null && maxPrice != null) {
                return cb.between(root.get("proborrow"), minPrice, maxPrice);
            } else if (minPrice != null) {
                return cb.greaterThanOrEqualTo(root.get("proborrow"), minPrice);
            } else {
                return cb.lessThanOrEqualTo(root.get("proborrow"), maxPrice);
            }
        };
    }
}
