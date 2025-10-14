package com.dodam.rent.repository;

import com.dodam.rent.entity.RentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RentRepository extends JpaRepository<RentEntity, Long> {

    // 리스트: LEFT JOIN FETCH 로 상품/회원이 null이어도 행 유지
    @Query("""
        select r
        from RentEntity r
        left join fetch r.product
        left join fetch r.member
        order by r.renNum desc
    """)
    List<RentEntity> findAllWithLeftJoinsOrderByRenNumDesc();

    // ✅ @Param 어노테이션 추가
    @Query("""
        select r
        from RentEntity r
        join fetch r.product
        join fetch r.member
        where r.renNum = :renNum
    """)
    Optional<RentEntity> findByRenNumWithJoins(@Param("renNum") Long renNum);
    
    List<RentEntity> findByMember_MidOrderByRenNumDesc(String mid);
    
}