package com.dodam.plan.repository;

import com.dodam.member.entity.MemberEntity;
import com.dodam.plan.Entity.PlanPaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

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

    default Optional<PlanPaymentEntity> findByMemberAndPayKey(MemberEntity member, String payKey) {
        return findByMidAndPayKey(member.getMid(), payKey);
    }
    default Optional<PlanPaymentEntity> findByMemberMidAndPayKey(String mid, String payKey) {
        return findByMidAndPayKey(mid, payKey);
    }
    default List<PlanPaymentEntity> findByMid(String mid) {
        return findByMidOrderByPayIdDesc(mid);
    }

    // ★ payId 기준 카드 메타 갱신
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
      update PlanPaymentEntity p
      set p.payBin   = coalesce(:bin,   p.payBin),
          p.payBrand = coalesce(:brand, p.payBrand),
          p.payLast4 = coalesce(:last4, p.payLast4),
          p.payPg    = coalesce(:pg,    p.payPg)
      where p.payId = :paymentId
    """)
    int updateCardMeta(@Param("paymentId") Long paymentId,
                       @Param("bin")   String bin,
                       @Param("brand") String brand,
                       @Param("last4") String last4,
                       @Param("pg")    String pg);

    // ★ billingKey 기준 카드 메타 갱신 (보조용)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
      update PlanPaymentEntity p
      set p.payBin   = coalesce(:bin,   p.payBin),
          p.payBrand = coalesce(:brand, p.payBrand),
          p.payLast4 = coalesce(:last4, p.payLast4),
          p.payPg    = coalesce(:pg,    p.payPg)
      where p.payKey = :payKey
    """)
    int updateCardMetaByKey(@Param("payKey") String payKey,
                            @Param("bin")   String bin,
                            @Param("brand") String brand,
                            @Param("last4") String last4,
                            @Param("pg")    String pg);

    default Optional<PlanPaymentEntity> findDefaultByMember(String mid) {
        List<PlanPaymentEntity> list = findByMidOrderByPayIdDesc(mid);
        if (list == null || list.isEmpty()) return java.util.Optional.empty();

        // 1) defaultYn = 'Y' 우선
        for (var p : list) {
            try {
                var m = PlanPaymentEntity.class.getMethod("getDefaultYn");
                Object v = m.invoke(p);
                if (v instanceof String s && "Y".equalsIgnoreCase(s)) return java.util.Optional.of(p);
            } catch (Throwable ignore) { }
        }
        // 2) isDefault = true
        for (var p : list) {
            try {
                var m = PlanPaymentEntity.class.getMethod("isDefault");
                Object v = m.invoke(p);
                if (v instanceof Boolean b && b) return java.util.Optional.of(p);
            } catch (Throwable ignore) { }
        }
        // 3) fallback: 최신 1건
        return Optional.of(list.get(0));
    }
}
