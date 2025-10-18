package com.dodam.main.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dodam.product.entity.ReviewEntity;
import java.util.List;

public interface MainFeedRepository extends JpaRepository<ReviewEntity, Long> {

	@Query(value = """
		    SELECT *
		      FROM (
		        SELECT 
		            r.REVNUM AS ID,
		            r.REVTITLE AS TITLE,
		            TO_CHAR(r.REVCRE, 'YYYY-MM-DD"T"HH24:MI:SS') AS REVCRE
		          FROM REVIEW r
		         ORDER BY r.REVCRE DESC, r.REVNUM DESC
		      )
		     WHERE ROWNUM <= :limit
		    """, nativeQuery = true)
		List<Object[]> findLatestReviews(@Param("limit") int limit);


    /** ✅ 공지/커뮤니티 최신 N개 */
    @Query(value = """
        SELECT *
          FROM (
            SELECT 
              b.BNUM   AS POST_ID,
              b.BSUB   AS TITLE,
              b.MNIC   AS AUTHOR,
              TO_CHAR(b.BDATE, 'YYYY-MM-DD"T"HH24:MI:SS') AS CREATED_AT,
              b.BCANUM AS BCANUM,
              c.BCANAME AS BCANAME
            FROM BOARD b
            LEFT JOIN BOARDCATEGORY c ON c.BCANUM = b.BCANUM
           WHERE b.BCANUM = :bcanum
           ORDER BY b.BDATE DESC, b.BNUM DESC
          )
         WHERE ROWNUM <= :limit
        """, nativeQuery = true)
    List<Object[]> findLatestBoardsByBcanum(@Param("bcanum") Long bcanum,
                                            @Param("limit") int limit);

    /** ✅ 인기 N개 (조회수 컬럼 없음 → 최신순 대체) */
    @Query(value = """
        SELECT *
          FROM (
            SELECT 
              b.BNUM   AS POST_ID,
              b.BSUB   AS TITLE,
              b.MNIC   AS AUTHOR,
              TO_CHAR(b.BDATE, 'YYYY-MM-DD"T"HH24:MI:SS') AS CREATED_AT,
              b.BCANUM AS BCANUM,
              c.BCANAME AS BCANAME
            FROM BOARD b
            LEFT JOIN BOARDCATEGORY c ON c.BCANUM = b.BCANUM
           WHERE b.BCANUM = :bcanum
           ORDER BY b.BDATE DESC, b.BNUM DESC
          )
         WHERE ROWNUM <= :limit
        """, nativeQuery = true)
    List<Object[]> findPopularBoardsByBcanum(@Param("bcanum") Long bcanum,
                                             @Param("limit") int limit);
}
