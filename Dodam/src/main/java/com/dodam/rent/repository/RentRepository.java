package com.dodam.rent.repository;

import com.dodam.rent.entity.RentEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RentRepository extends JpaRepository<RentEntity, Long> {

    /**
     * 주문 리스트 조회용
     * - product, member를 함께 로딩해서 N+1 방지
     * - 최신 순 정렬(renNum DESC)
     */
    @EntityGraph(attributePaths = {"product", "member"})
    List<RentEntity> findAllByOrderByRenNumDesc();

    /**
     * 주문 상세 조회용
     * - fetch join으로 product, member까지 한 번에 로딩
     */
    @Query("select r from RentEntity r " +
           "join fetch r.product " +
           "join fetch r.member " +
           "where r.renNum = :renNum")
    Optional<RentEntity> findByRenNumWithJoins(@Param("renNum") Long renNum);
}
