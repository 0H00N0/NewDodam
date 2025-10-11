// src/main/java/com/dodam/plan/service/PlanBillingKeyService.java
package com.dodam.plan.service;

import com.dodam.plan.Entity.PlanPaymentEntity;
import com.dodam.plan.dto.PlanCardDTO;
import com.dodam.plan.repository.PlanPaymentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanBillingKeyService {

    private final PlanPaymentGatewayService gateway;
    private final PlanPaymentRepository repo;

    @Transactional
    public void registerAndSave(String mid, String customerKey, String billingKey) {
        if (!StringUtils.hasText(billingKey)) return; // ❗ 예외 던지지 않음

        try {
            JsonNode bk = gateway.getBillingKey(billingKey);
            String brand = text(bk.path("card"), "brand", "brandName");
            String bin   = text(bk.path("card"), "bin");
            String last4 = text(bk.path("card"), "last4", "numberLast4");
            String pg    = text(bk, "pgProvider", "provider", "pg");

            PlanPaymentEntity e = repo.findByMidAndPayKey(mid, billingKey)
                    .orElseGet(PlanPaymentEntity::new);

            e.setMid(mid);
            e.setPayKey(billingKey);
            e.setPayCustomer(customerKey);
            e.setPayBrand(brand);
            e.setPayBin(bin);
            e.setPayLast4(last4);
            e.setPayPg(pg);
            e.setPayRaw(bk.toPrettyString());
            if (e.getPayCreatedAt() == null) e.setPayCreatedAt(LocalDateTime.now());
            e.setPayActive(true);

            repo.save(e);
            log.info("[BillingKey] upsert mid={} key={} brand={} last4={}", mid, billingKey, brand, last4);
        } catch (Exception ex) {
            log.warn("[BillingKey] register failed mid={} key={} : {}", mid, billingKey, ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<PlanCardDTO> listCards(String mid) {
        return repo.findByMidOrderByPayIdDesc(mid)
                .stream()
                .filter(PlanPaymentEntity::isPayActive)
                .map(PlanCardDTO::from)
                .toList();
    }

    /** ✅ 예외 없이 조용히 비활성화: 영향행수만 로깅 */
    @Transactional
    public void deactivateById(String mid, Long payId) {
        if (!StringUtils.hasText(mid) || payId == null) return;
        int rows = repo.deactivateById(mid, payId);
        log.info("[BillingKey] deactivateById mid={} payId={} rows={}", mid, payId, rows);
    }

    @Transactional
    public void deactivateByKey(String mid, String payKey) {
        if (!StringUtils.hasText(mid) || !StringUtils.hasText(payKey)) return;
        int rows = repo.deactivateByKey(mid, payKey);
        log.info("[BillingKey] deactivateByKey mid={} key={} rows={}", mid, payKey, rows);
    }

    private static String text(JsonNode n, String... keys) {
        for (String k : keys) {
            String v = n.path(k).asText(null);
            if (StringUtils.hasText(v)) return v;
        }
        return null;
    }
}
