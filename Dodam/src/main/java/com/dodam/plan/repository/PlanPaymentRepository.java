package com.dodam.plan.repository;

import com.dodam.member.entity.MemberEntity;
import com.dodam.plan.Entity.PlanPaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    // 🔹 추가: payId + mid 로 단건 조회 (소유자 검증용)
    Optional<PlanPaymentEntity> findByPayIdAndMid(Long payId, String mid);

    // ✅ member 조인 제거: 엔티티에 실제 존재하는 필드(mid, payCustomer)만 조건에 사용
    @Query("""
       select p
         from PlanPaymentEntity p
        where p.mid = :key
           or p.payCustomer = :key
        order by p.payId desc
    """)
    List<PlanPaymentEntity> findAllByAnyKey(@Param("key") String key);
    
 // ✅ 예외 없이 조용히 비활성화 (영향받은 행 수 반환)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update PlanPaymentEntity p
           set p.payActive = false
         where p.payId = :payId
           and p.mid   = :mid
           and p.payActive = true
    """)
    int deactivateById(@Param("mid") String mid, @Param("payId") Long payId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update PlanPaymentEntity p
           set p.payActive = false
         where p.payKey = :payKey
           and p.mid    = :mid
           and p.payActive = true
    """)
    int deactivateByKey(@Param("mid") String mid, @Param("payKey") String payKey);

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
