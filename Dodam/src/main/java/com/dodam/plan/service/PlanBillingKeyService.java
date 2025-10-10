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
        if (!StringUtils.hasText(billingKey))
            throw new IllegalArgumentException("billingKey required");

        try {
            JsonNode bk = gateway.getBillingKey(billingKey);
            String brand = text(bk.path("card"), "brand", "brandName");
            String bin = text(bk.path("card"), "bin");
            String last4 = text(bk.path("card"), "last4", "numberLast4");
            String pg = text(bk, "pgProvider", "provider", "pg");
            String status = text(bk, "status");

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
            e.setPayCreatedAt(e.getPayCreatedAt() == null ? LocalDateTime.now() : e.getPayCreatedAt());

            repo.save(e);
            log.info("[BillingKey] saved mid={} billingKey={} brand={} last4={}",
                    mid, billingKey, brand, last4);
        } catch (Exception e) {
            log.error("[BillingKey] register failed: {}", e.getMessage(), e);
            throw new RuntimeException("카드 등록 실패");
        }
    }

    public List<PlanCardDTO> listCards(String mid) {
        return repo.findAllByMid(mid)
                .stream()
                .map(PlanCardDTO::from)
                .toList();
    }

    private static String text(JsonNode n, String... keys) {
        for (String k : keys) {
            String v = n.path(k).asText(null);
            if (StringUtils.hasText(v)) return v;
        }
        return null;
    }
}
