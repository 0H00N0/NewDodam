// com/dodam/main/repository/MainFeedRepository.java
package com.dodam.main.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dodam.product.entity.ReviewEntity;
import java.util.List;

public interface MainFeedRepository extends JpaRepository<ReviewEntity, Long> {

    /** 최근 리뷰 (상품 대표 이미지 포함) */
    @Query(value = """
        SELECT *
          FROM (
            SELECT
              r.REVNUM       AS REVID,
              r.REVTITLE     AS TITLE,
              r.REVSCORE     AS SCORE,
              TO_CHAR(r.REVCRE, 'YYYY-MM-DD"T"HH24:MI:SS') AS CREATED_AT,
              p.PRONUM       AS PROID,
              p.PRONAME      AS PRONAME,
              (SELECT MAX(pi.PROURL)
                 FROM DODAM.PRODUCTIMAGE pi
                WHERE pi.PRONUM = p.PRONUM) AS IMAGE_URL
            FROM DODAM.REVIEW r
            JOIN DODAM.PRODUCT p ON p.PRONUM = r.PRONUM
            ORDER BY r.REVCRE DESC
          )
         WHERE ROWNUM <= :limit
        """, nativeQuery = true)
    List<Object[]> findLatestReviews(@Param("limit") int limit);

    /** 카테고리 최신 글 (공지/커뮤니티 공통) */
    @Query(value = """
        SELECT *
          FROM (
            SELECT
              b.BNUM   AS POSTID,
              b.BTITLE AS TITLE,
              b.MNIC   AS AUTHOR,
              TO_CHAR(b.BDATE, 'YYYY-MM-DD"T"HH24:MI:SS') AS CREATED_AT,
              b.BCANUM  AS BCANUM,
              c.BCANAME AS BCANAME
            FROM DODAM.BOARD b
            LEFT JOIN DODAM.BOARDCATEGORY c ON c.BCANUM = b.BCANUM
           WHERE b.BCANUM = :bcanum
           ORDER BY b.BDATE DESC
          )
         WHERE ROWNUM <= :limit
        """, nativeQuery = true)
    List<Object[]> findLatestBoardsByBcanum(@Param("bcanum") Long bcanum, @Param("limit") int limit);

    /** 카테고리 인기글 (임시: 최신순 → 좋아요/조회 테이블 있으면 ORDER BY 교체) */
    @Query(value = """
        SELECT *
          FROM (
            SELECT
              b.BNUM   AS POSTID,
              b.BTITLE AS TITLE,
              b.MNIC   AS AUTHOR,
              TO_CHAR(b.BDATE, 'YYYY-MM-DD"T"HH24:MI:SS') AS CREATED_AT,
              b.BCANUM  AS BCANUM,
              c.BCANAME AS BCANAME
            FROM DODAM.BOARD b
            LEFT JOIN DODAM.BOARDCATEGORY c ON c.BCANUM = b.BCANUM
           WHERE b.BCANUM = :bcanum
           ORDER BY b.BDATE DESC  -- TODO: 좋아요/조회 기준으로 교체 가능
          )
         WHERE ROWNUM <= :limit
        """, nativeQuery = true)
    List<Object[]> findPopularBoardsByBcanum(@Param("bcanum") Long bcanum, @Param("limit") int limit);
}
