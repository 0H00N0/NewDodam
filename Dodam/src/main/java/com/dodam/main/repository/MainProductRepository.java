package com.dodam.main.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dodam.product.entity.ProductEntity;

public interface MainProductRepository extends JpaRepository<ProductEntity, Long> {

    /**
     * 신상품(이름 단위)
     * - 같은 PRONAME 내에서 최신 PROCRE (동률이면 PRONUM 큰 것) 1개 대표
     * - 대표들을 최신순으로 정렬해서 상위 N
     * - 가격=PRODUCT.PROBORROW, 이미지=PRODUCTIMAGE.PROURL(대표 1장)
     * - ⚠️ 스키마 접두사 DODAM. 명시
     */
    @Query(value = """
        SELECT *
        FROM (
          SELECT
            t.NAME,
            t.PRONUM   AS PROID,
            t.IMAGE_URL,
            t.PRICE,
            TO_CHAR(t.PROCRE, 'YYYY-MM-DD"T"HH24:MI:SS') AS PROCRE
          FROM (
            SELECT
              p.PRONAME AS NAME,
              p.PRONUM,
              p.PROCRE,
              p.PROBORROW AS PRICE,
              (SELECT MAX(pi.PROURL)
                 FROM DODAM.PRODUCTIMAGE pi
                WHERE pi.PRONUM = p.PRONUM) AS IMAGE_URL,
              ROW_NUMBER() OVER (
                PARTITION BY p.PRONAME
                ORDER BY p.PROCRE DESC, p.PRONUM DESC
              ) AS RN
            FROM DODAM.PRODUCT p
          ) t
          WHERE t.RN = 1
          ORDER BY t.PROCRE DESC
        )
        WHERE ROWNUM <= :limit
        """, nativeQuery = true)
    List<Object[]> findNewProductsByName(@Param("limit") int limit);

    /**
     * 인기상품(이름 단위, 임시: 대여 집계 없이 보유 개수로 정렬)
     * - RENT 테이블 의존 제거 (후에 정확한 테이블/컬럼 확인되면 CNT 부분 교체)
     * - 같은 PRONAME 내 대표는 최신 PROCRE → PRONUM 큰 것
     * - 이름별 보유 개수(= PRONUM 개수) 합계로 인기순 정렬
     */
    @Query(value = """
        WITH per_item AS (
          SELECT
            p.PRONAME AS NAME,
            p.PRONUM  AS PRONUM,
            p.PROCRE  AS PROCRE,
            p.PROBORROW AS PRICE,
            (SELECT MAX(pi.PROURL)
               FROM DODAM.PRODUCTIMAGE pi
              WHERE pi.PRONUM = p.PRONUM) AS IMAGE_URL,
            1 AS CNT
          FROM DODAM.PRODUCT p
        ),
        name_total AS (
          SELECT NAME, SUM(CNT) AS TOTAL_CNT
          FROM per_item
          GROUP BY NAME
        ),
        rep AS (
          SELECT
            NAME,
            MAX(PRONUM)    KEEP (DENSE_RANK LAST ORDER BY PROCRE, PRONUM) AS REP_PRONUM,
            MAX(IMAGE_URL) KEEP (DENSE_RANK LAST ORDER BY PROCRE, PRONUM) AS REP_IMAGE_URL,
            MAX(PRICE)     KEEP (DENSE_RANK LAST ORDER BY PROCRE, PRONUM) AS REP_PRICE
          FROM per_item
          GROUP BY NAME
        )
        SELECT *
        FROM (
          SELECT
            t.NAME           AS NAME,
            r.REP_PRONUM     AS PROID,
            r.REP_IMAGE_URL  AS IMAGE_URL,
            r.REP_PRICE      AS PRICE,
            t.TOTAL_CNT      AS RENTCOUNT  -- 임시로 보유개수 사용
          FROM name_total t
          JOIN rep r ON r.NAME = t.NAME
          ORDER BY t.TOTAL_CNT DESC, r.REP_PRONUM DESC
        )
        WHERE ROWNUM <= :limit
        """, nativeQuery = true)
    List<Object[]> findPopularProductsByName(@Param("limit") int limit);
}
