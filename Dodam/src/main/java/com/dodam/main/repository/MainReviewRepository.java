// src/main/java/com/dodam/review/repository/ReviewRepository.java
package com.dodam.main.repository;

import com.dodam.product.entity.ReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MainReviewRepository extends JpaRepository<ReviewEntity, Long> {

    // ✅ pronum 기준으로 그룹 카운트 (네이티브: Oracle/H2 모두 호환 쉬움)
    @Query(value = """
        SELECT r.pronum AS product_id, COUNT(*) AS cnt
          FROM review r
         WHERE r.pronum IN (:ids)
         GROUP BY r.pronum
        """, nativeQuery = true)
    List<Object[]> countByPronumIn(@Param("ids") List<Long> ids);
}
