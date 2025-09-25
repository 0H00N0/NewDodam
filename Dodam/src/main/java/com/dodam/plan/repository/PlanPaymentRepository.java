package com.dodam.plan.repository;

import com.dodam.member.entity.MemberEntity;
import com.dodam.plan.Entity.PlanPaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

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

    default Optional<PlanPaymentEntity> findDefaultByMember(String mid) {
        List<PlanPaymentEntity> list = findByMidOrderByPayIdDesc(mid);
        if (list == null || list.isEmpty()) return Optional.empty();

        // 1) defaultYn = 'Y' 우선
        for (var p : list) {
            try {
                var m = PlanPaymentEntity.class.getMethod("getDefaultYn");
                Object v = m.invoke(p);
                if (v instanceof String s && "Y".equalsIgnoreCase(s)) return Optional.of(p);
            } catch (Throwable ignore) { }
        }
        // 2) isDefault = true
        for (var p : list) {
            try {
                var m = PlanPaymentEntity.class.getMethod("isDefault");
                Object v = m.invoke(p);
                if (v instanceof Boolean b && b) return Optional.of(p);
            } catch (Throwable ignore) { }
        }
        // 3) fallback: 최신 1건
        return Optional.of(list.get(0));
    }
}
