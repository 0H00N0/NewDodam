// src/main/java/com/dodam/plan/service/PlanPaymentProfileService.java
package com.dodam.plan.service;

import com.dodam.plan.Entity.PlanPaymentEntity;
import com.dodam.plan.dto.PlanCardMeta;
import com.dodam.plan.dto.PlanPaymentRegisterReq;
import com.dodam.plan.repository.PlanPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanPaymentProfileService {

    private final PlanPaymentRepository paymentRepo;
    private final PlanPaymentGatewayService pgSvc;

    @Transactional
    public PlanPaymentEntity upsert(String mid, PlanPaymentRegisterReq req) {
        if (!StringUtils.hasText(mid)) {
            throw new IllegalArgumentException("mid required");
        }
        if (req == null || !StringUtils.hasText(req.getBillingKey())) {
            throw new IllegalArgumentException("billingKey required");
        }

        // 1) rawJson 파싱은 '옵션' — 실패/누락해도 진행
        PlanCardMeta meta = null;
        final String rawJson = safeTrim(req.getRawJson());
        if (StringUtils.hasText(rawJson)) {
            try {
                // pgSvc가 null일 일은 거의 없지만, 혹시 모르니 방어
                if (pgSvc != null) meta = pgSvc.extractCardMeta(rawJson);
            } catch (Exception e) {
                log.warn("extractCardMeta failed: {}", e.toString());
                meta = null; // 파싱 실패는 무시하고 계속
            }
        }

        // 2) 필드 값 결정: 요청값(req) > 메타(meta) > fallback(null)
        String customerId = coalesceNonBlank(req.getCustomerId(), opt(meta, PlanCardMeta::getCustomerId));
        String brand      = coalesceNonBlank(req.getBrand(),      opt(meta, PlanCardMeta::getBrand));
        String bin        = coalesceNonBlank(req.getBin(),        opt(meta, PlanCardMeta::getBin));
        String last4      = coalesceNonBlank(req.getLast4(),      opt(meta, PlanCardMeta::getLast4));
        String pg         = coalesceNonBlank(req.getPg(),         opt(meta, PlanCardMeta::getPg));

        // 3) (mid, billingKey) 조회 → 있으면 갱신, 없으면 신규 (중복 insert 방지)
        PlanPaymentEntity entity = paymentRepo.findByMemberMidAndPayKey(mid, req.getBillingKey())
                .orElseGet(() -> {
                    PlanPaymentEntity e = new PlanPaymentEntity();
                    e.setMid(mid);
                    e.setPayKey(req.getBillingKey());
                    // 생성시각 필드가 있을 수도/없을 수도 있으므로 리플렉션은 지양, 메서드 있으면 호출
                    try {
                        e.getClass().getMethod("setPayCreatedAt", LocalDateTime.class)
                                .invoke(e, LocalDateTime.now());
                    } catch (Exception ignore) {}
                    return e;
                });

        // 4) 값 갱신 — 비어있지 않은 값만 덮어쓰기
        if (StringUtils.hasText(customerId)) entity.setPayCustomer(customerId);
        if (!StringUtils.hasText(entity.getPayCustomer())) entity.setPayCustomer(mid); // 최종 fallback

        if (StringUtils.hasText(brand)) entity.setPayBrand(brand);
        if (StringUtils.hasText(bin))   entity.setPayBin(bin);
        if (StringUtils.hasText(last4)) entity.setPayLast4(last4);
        if (StringUtils.hasText(pg))    {
            try { entity.getClass().getMethod("setPayPg", String.class).invoke(entity, pg); }
            catch (Exception ignore) {}
        }

        if (StringUtils.hasText(rawJson)) {
            entity.setPayRaw(rawJson);
        }

        // 5) 저장 (중복 없이 upsert)
        return paymentRepo.saveAndFlush(entity);
    }

    // ===== util =====
    private static String safeTrim(String s) {
        return (s == null) ? null : s.trim();
    }
    @SafeVarargs
    private static String coalesceNonBlank(String... values) {
        if (values == null) return null;
        for (String v : values) if (StringUtils.hasText(v)) return v;
        return null;
    }
    private static <T> String opt(T obj, java.util.function.Function<T, String> f) {
        try {
            if (obj == null) return null;
            String v = f.apply(obj);
            return StringUtils.hasText(v) ? v : null;
        } catch (Exception e) {
            return null;
        }
    }
}
