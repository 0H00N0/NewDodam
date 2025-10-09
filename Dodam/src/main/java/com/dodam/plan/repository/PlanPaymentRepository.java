package com.dodam.plan.repository;

import com.dodam.member.entity.MemberEntity;
import com.dodam.plan.Entity.PlanPaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlanPaymentRepository extends JpaRepository<PlanPaymentEntity, Long> {

    List<PlanPaymentEntity> findAllByMid(String mid);
    boolean existsByMidAndPayKey(String mid, String payKey);
    List<PlanPaymentEntity> findByMidOrderByPayIdDesc(String mid);
    Optional<PlanPaymentEntity> findTop1ByMidOrderByPayIdDesc(String mid);
    Optional<PlanPaymentEntity> findTopByMidOrderByPayIdDesc(String mid);

    Optional<PlanPaymentEntity> findByPayKey(String payKey);
    Optional<PlanPaymentEntity> findByMidAndPayKey(String mid, String payKey);

    // ✅ member 조인 제거: 엔티티에 실제 존재하는 필드(mid, payCustomer)만 조건에 사용
    @Query("""
       select p
         from PlanPaymentEntity p
        where p.mid = :key
           or p.payCustomer = :key
        order by p.payId desc
    """)
    List<PlanPaymentEntity> findAllByAnyKey(@Param("key") String key);

    /* ---- 편의 메서드 ---- */
    default Optional<PlanPaymentEntity> findByMemberAndPayKey(MemberEntity member, String payKey) {
        return findByMidAndPayKey(member.getMid(), payKey);
    }
    default Optional<PlanPaymentEntity> findByMemberMidAndPayKey(String mid, String payKey) {
        return findByMidAndPayKey(mid, payKey);
    }
    default List<PlanPaymentEntity> findByMid(String mid) {
        return findByMidOrderByPayIdDesc(mid);
    }
    default Optional<PlanPaymentEntity> findDefaultByMember(String mid) {
        List<PlanPaymentEntity> list = findByMidOrderByPayIdDesc(mid);
        if (list == null || list.isEmpty()) return Optional.empty();
        for (var p : list) {
            try {
                var m = PlanPaymentEntity.class.getMethod("getDefaultYn");
                Object v = m.invoke(p);
                if (v instanceof String s && "Y".equalsIgnoreCase(s)) return Optional.of(p);
            } catch (Throwable ignore) { }
        }
        for (var p : list) {
            try {
                var m = PlanPaymentEntity.class.getMethod("isDefault");
                Object v = m.invoke(p);
                if (v instanceof Boolean b && b) return Optional.of(p);
            } catch (Throwable ignore) { }
        }
        return Optional.of(list.get(0));
    }
}
