package com.dodam.discount.repository;

import com.dodam.discount.entity.Discount;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DiscountRepository extends JpaRepository<Discount, Long> {

    /**
     * ptermId로 구독할인(disLevel=2)의 최대 할인율(disValue) 조회
     */
    @Query("""
        select max(d.disValue)
        from Discount d
        where d.ptermId.ptermId = :ptermId
          and d.disLevel = 2
        """)
    Optional<Integer> findRateByPterm(@Param("ptermId") Long ptermId);

    /**
     * months(개월 수)로 구독할인(disLevel=2)의 최대 할인율(disValue) 조회
     */
    @Query("""
        select max(d.disValue)
        from Discount d
        where d.ptermId.ptermMonth = :months
          and d.disLevel = 2
        """)
    Optional<Integer> findRateByMonths(@Param("months") int months);
}
