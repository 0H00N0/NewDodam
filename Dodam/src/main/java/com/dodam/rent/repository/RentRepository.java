package com.dodam.rent.repository;

import com.dodam.rent.entity.RentEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RentRepository extends JpaRepository<RentEntity, Long> {

    /**
     * 목록 조회: Product/Member를 fetch join으로 한 번에 로딩
     * - LEFT JOIN FETCH: 연관값이 null이어도 행 유지
     * - DISTINCT: 조인으로 인한 중복 방지 (JPA가 메모리에서 중복 제거)
     * - 주의: 페이징과 함께 쓰지 말 것 (필요시 @EntityGraph + findAll(Pageable) 전략 권장)
     */
    @Query("""
        select distinct r
        from RentEntity r
        left join fetch r.product
        left join fetch r.member
        order by r.renNum desc
    """)
    List<RentEntity> findAllWithLeftJoinsOrderByRenNumDesc();

    /**
     * 단건 조회: 필수 연관 포함 즉시 로딩
     * - 엔티티 매핑상 product/member가 nullable = false 이므로 inner join fetch 사용
     */
    @Query("""
        select r
        from RentEntity r
        join fetch r.product
        join fetch r.member
        where r.renNum = :renNum
    """)
    Optional<RentEntity> findByRenNumWithJoins(@Param("renNum") Long renNum);

    /**
     * 회원 아이디(mid)로 필터링하여 최신순 목록
     * - 파생 쿼리 그대로 사용하되 N+1 방지를 위해 연관을 EntityGraph로 즉시 로딩
     * - 페이징 붙여도 안전 (fetch join이 아니라 count 분리 이슈 없음)
     */
    @EntityGraph(attributePaths = {"product", "member"})
    List<RentEntity> findByMember_MidOrderByRenNumDesc(String mid);
}
