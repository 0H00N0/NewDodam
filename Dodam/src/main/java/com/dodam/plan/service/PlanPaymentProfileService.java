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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanPaymentProfileService {

    private final PlanPaymentRepository paymentRepo;
    private final PlanPaymentGatewayService pgSvc;

    /* ===================== Core Upsert ===================== */

    @Transactional
    public PlanPaymentEntity upsert(String mid, PlanPaymentRegisterReq req) {
        if (!StringUtils.hasText(mid)) throw new IllegalArgumentException("mid required");
        if (req == null || !StringUtils.hasText(req.getBillingKey())) throw new IllegalArgumentException("billingKey required");

        PlanCardMeta meta = null;
        final String rawJson = safeTrim(req.getRawJson());
        if (StringUtils.hasText(rawJson)) {
            try { if (pgSvc != null) meta = pgSvc.extractCardMeta(rawJson); }
            catch (Exception e) { log.warn("extractCardMeta failed: {}", e.toString()); meta = null; }
        }

        String customerId = coalesceNonBlank(req.getCustomerId(), opt(meta, PlanCardMeta::getCustomerId), mid);
        String brand      = coalesceNonBlank(req.getBrand(),      opt(meta, PlanCardMeta::getBrand));
        String bin        = coalesceNonBlank(req.getBin(),        opt(meta, PlanCardMeta::getBin));
        String last4      = coalesceNonBlank(req.getLast4(),      opt(meta, PlanCardMeta::getLast4));
        String pg         = coalesceNonBlank(req.getPg(),         opt(meta, PlanCardMeta::getPg), "TossPayments");

        PlanPaymentEntity entity = paymentRepo.findByMemberMidAndPayKey(mid, req.getBillingKey())
                .orElseGet(() -> {
                    PlanPaymentEntity e = new PlanPaymentEntity();
                    e.setMid(mid);
                    e.setPayKey(req.getBillingKey());
                    try { e.getClass().getMethod("setPayCreatedAt", LocalDateTime.class).invoke(e, LocalDateTime.now()); }
                    catch (Exception ignore) {}
                    return e;
                });

        if (StringUtils.hasText(customerId)) entity.setPayCustomer(customerId);
        if (!StringUtils.hasText(entity.getPayCustomer())) entity.setPayCustomer(mid);

        if (StringUtils.hasText(brand)) entity.setPayBrand(brand);
        if (StringUtils.hasText(bin))   entity.setPayBin(bin);
        if (StringUtils.hasText(last4)) entity.setPayLast4(last4);
        if (StringUtils.hasText(pg))    {
            try { entity.getClass().getMethod("setPayPg", String.class).invoke(entity, pg); }
            catch (Exception ignore) { entity.setPayPg(pg); }
        }
        if (StringUtils.hasText(rawJson)) entity.setPayRaw(rawJson);

        return paymentRepo.saveAndFlush(entity);
    }

    /* ===================== 조회/수정 API ===================== */

    /** 활성 카드 요약(최신 1건) */
    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> getActiveCardMeta(String mid) {
        if (!StringUtils.hasText(mid)) return Optional.empty();
        List<PlanPaymentEntity> list = paymentRepo.findAllByAnyKey(mid);
        if (list.isEmpty()) return Optional.empty();
        PlanPaymentEntity p = list.get(0);
        return Optional.of(Map.of(
                "payId",  p.getPayId(),
                "brand",  p.getPayBrand(),
                "last4",  p.getPayLast4(),
                "pg",     p.getPayPg(),
                "bin",    p.getPayBin(),
                "mid",    p.getMid()
        ));
    }

    /** 카드 목록(최신순) */
    @Transactional(readOnly = true)
    public List<Map<String,Object>> listCards(String mid) {
        if (!StringUtils.hasText(mid)) return List.of();
        return paymentRepo.findAllByAnyKey(mid).stream().map(p -> {
            Map<String,Object> m = new HashMap<>();
            m.put("payId",  p.getPayId());
            m.put("brand",  p.getPayBrand());
            m.put("last4",  p.getPayLast4());
            m.put("pg",     p.getPayPg());
            m.put("bin",    p.getPayBin());
            m.put("mid",    p.getMid());
            m.put("payKey", p.getPayKey());
            return m;
        }).toList();
    }

    /** 카드 교체(신규 등록/갱신) */
    @Transactional
    public PlanPaymentEntity changeCard(String mid,
                                        String billingKey,
                                        String brand,
                                        String bin,
                                        String last4,
                                        String pg,
                                        String raw) {
        if (!StringUtils.hasText(mid)) throw new IllegalArgumentException("mid required");
        if (!StringUtils.hasText(billingKey)) throw new IllegalArgumentException("billingKey required");

        PlanPaymentRegisterReq req = new PlanPaymentRegisterReq();
        req.setBillingKey(billingKey);
        req.setBrand(brand);
        req.setBin(bin);
        req.setLast4(last4);
        req.setPg(normalizePg(pg));
        req.setRawJson(raw);
        req.setCustomerId(mid);

        return upsert(mid, req);
    }

    /** 활성 카드 삭제(=비활성화) : 최신 1건 삭제 */
    @Transactional
    public boolean removeActive(String mid) {
        if (!StringUtils.hasText(mid)) return false;
        List<PlanPaymentEntity> list = paymentRepo.findAllByAnyKey(mid);
        if (list.isEmpty()) return false;
        paymentRepo.delete(list.get(0));
        return true;
    }

    /* ===================== 하위호환 (mnum: Long) ===================== */
    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> getActiveCardMeta(Long mnum) { return getActiveCardMeta(mnumToMid(mnum)); }
    @Transactional public PlanPaymentEntity changeCard(Long mnum, String billingKey, String brand, String bin, String last4, String pg, String raw) {
        return changeCard(mnumToMid(mnum), billingKey, brand, bin, last4, pg, raw);
    }
    @Transactional public boolean removeActive(Long mnum) { return removeActive(mnumToMid(mnum)); }

    /* ===================== utils ===================== */
    private static String normalizePg(String v){
        if (!StringUtils.hasText(v)) return "TossPayments";
        String u = v.trim();
        if ("TOSS".equalsIgnoreCase(u) || "TOSSPAYMENTS".equalsIgnoreCase(u)) return "TossPayments";
        return u;
    }
    private static String mnumToMid(Long mnum){ return mnum == null ? null : String.valueOf(mnum); }
    private static String safeTrim(String s) { return (s == null) ? null : s.trim(); }
    @SafeVarargs private static String coalesceNonBlank(String... values) {
        if (values == null) return null;
        for (String v : values) if (StringUtils.hasText(v)) return v;
        return null;
    }
    private static <T> String opt(T obj, java.util.function.Function<T, String> f) {
        try { if (obj == null) return null; String v = f.apply(obj); return StringUtils.hasText(v) ? v : null; }
        catch (Exception e) { return null; }
    }
}
