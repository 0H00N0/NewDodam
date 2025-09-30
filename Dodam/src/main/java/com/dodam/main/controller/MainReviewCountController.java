// src/main/java/com/dodam/review/controller/ReviewCountController.java
package com.dodam.main.controller;

import com.dodam.main.repository.MainReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class MainReviewCountController {

    private final MainReviewRepository reviewRepository;

    /** 예) GET /api/reviews/count?ids=101,102,103
     *  응답: { "counts": { "101": 3, "102": 0, "103": 5 } }
     */
    @GetMapping("/api/reviews/count")
    public Map<String, Object> countByProductIds(@RequestParam("ids") List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of("counts", Collections.emptyMap());
        }
        List<Object[]> rows = reviewRepository.countByPronumIn(ids);

        Map<String, Long> counts = rows.stream().collect(Collectors.toMap(
                r -> String.valueOf(((Number) r[0]).longValue()),
                r -> ((Number) r[1]).longValue()
        ));

        // 요청에 포함되었지만 리뷰가 0개인 상품도 0으로 채워 반환
        for (Long id : ids) {
            counts.putIfAbsent(String.valueOf(id), 0L);
        }

        return Map.of("counts", counts);
    }
}
