package com.dodam.main.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dodam.product.entity.ProductEntity;

public interface MainProductRepository extends JpaRepository<ProductEntity, Long> {

    /**
     * 신상품(이름 단위)
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
     * 인기상품(이름 단위, 임시: 보유 개수 기준)
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
            t.TOTAL_CNT      AS RENTCOUNT
          FROM name_total t
          JOIN rep r ON r.NAME = t.NAME
          ORDER BY t.TOTAL_CNT DESC, r.REP_PRONUM DESC
        )
        WHERE ROWNUM <= :limit
        """, nativeQuery = true)
    List<Object[]> findPopularProductsByName(@Param("limit") int limit);

    /**
     * ✅ 단건 상세(대표 이미지 1장 포함)
     */
    @Query(value = """
        SELECT
          p.PRONUM   AS PROID,
          p.PRONAME  AS NAME,
          p.PROBORROW AS PRICE,
          TO_CHAR(p.PROCRE, 'YYYY-MM-DD"T"HH24:MI:SS') AS PROCRE,
          (SELECT MAX(pi.PROURL)
             FROM DODAM.PRODUCTIMAGE pi
            WHERE pi.PRONUM = p.PRONUM) AS IMAGE_URL
        FROM DODAM.PRODUCT p
        WHERE p.PRONUM = :proId
        """, nativeQuery = true)
    Object[] findProductBasicById(@Param("proId") Long proId);
    
    /** ✅ 해당 상품의 상세 이미지 URL 목록 */
    @Query(value = """
        SELECT pi.PROURL
          FROM DODAM.PRODUCTIMAGE pi
         WHERE pi.PRONUM = :proId
           AND ROWNUM <= :limit
        """, nativeQuery = true)
    List<String> findProductImageUrls(@Param("proId") Long proId, @Param("limit") int limit);
    
    /** 해당 PRONUM의 모든 이미지 URL (대표/순번 기준 정렬) */
    @Query(value = """
        SELECT pi.PROURL
          FROM DODAM.PRODUCTIMAGE pi
         WHERE pi.PRONUM = :proId
         ORDER BY pi.PROIMGNUM
        """, nativeQuery = true)
    List<String> findAllImageUrlsByProId(@Param("proId") Long proId);
    
    /** 상위 N개만 */
    @Query(value = """
        SELECT PROURL
          FROM (
            SELECT pi.PROURL
              FROM DODAM.PRODUCTIMAGE pi
             WHERE pi.PRONUM = :proId
             ORDER BY pi.PROIMGNUM
          )
         WHERE ROWNUM <= :limit
        """, nativeQuery = true)
    List<String> findImageUrlsByProIdLimited(@Param("proId") Long proId,
                                             @Param("limit") int limit);
}
