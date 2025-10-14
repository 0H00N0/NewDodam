// src/main/java/com/dodam/plan/scheduler/PlanAutoRenewScheduler.java
package com.dodam.plan.scheduler;

import com.dodam.plan.Entity.PlanInvoiceEntity;
import com.dodam.plan.Entity.PlanMember;
import com.dodam.plan.Entity.PlanPriceEntity;
import com.dodam.plan.Entity.PlanTermsEntity;
import com.dodam.plan.enums.PlanEnums;
import com.dodam.plan.repository.PlanInvoiceRepository;
import com.dodam.plan.repository.PlanMemberRepository;
import com.dodam.plan.service.PlanPortoneClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlanAutoRenewScheduler {

    private final PlanMemberRepository planMemberRepo;
    private final PlanInvoiceRepository invoiceRepo;
    private final PlanPortoneClientService portone;

    /**
     * ✅ 매일 새벽 4시 (KST), pmNextBil ≤ 오늘 23:59:59 이고 ACTIVE 이며
     *    기간말 해지 예약이 아닌 구독을 찾아 자동결제 예약.
     */
    @Transactional
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void autoRenewTodayMembers() {
        LocalDate today = LocalDate.now();
        LocalDateTime until = today.plusDays(1).atStartOfDay(); // 오늘의 끝(내일 00:00) 전까지

        List<PlanMember> targets = planMemberRepo
                .findByPmNextBilBeforeAndPmStatusAndCancelAtPeriodEndFalse(
                        until, PlanEnums.PmStatus.ACTIVE);

        if (targets.isEmpty()) {
            log.info("[AutoRenew] 결제 예정 회원 없음");
            return;
        }
        log.info("[AutoRenew] {}명 자동결제 예약 시작", targets.size());

        for (PlanMember pm : targets) {
            try {
                if (pm.getPayment() == null || pm.getPayment().getPayKey() == null) {
                    log.warn("[AutoRenew] pmId={} 결제수단 없음 → skip", pm.getPmId());
                    continue;
                }

                // 1) next* 예약 변경값을 우선 적용
                PlanPriceEntity price = (pm.getNextPrice() != null) ? pm.getNextPrice() : pm.getPrice();
                PlanTermsEntity terms = (pm.getNextTerms() != null) ? pm.getNextTerms() : pm.getTerms();

                if (price == null) {
                    log.warn("[AutoRenew] pmId={} price 없음 → skip", pm.getPmId());
                    continue;
                }

                // 2) 개월수/금액/통화 계산 (널 안전)
                int months = 1;
                try {
                    if (terms != null && terms.getPtermMonth() != null) {
                        months = Math.max(1, terms.getPtermMonth());
                    } else if (price.getPterm() != null && price.getPterm().getPtermMonth() != null) {
                        months = Math.max(1, price.getPterm().getPtermMonth());
                    }
                } catch (Exception ignore) {}

                BigDecimal amount = (price.getPpriceAmount() != null) ? price.getPpriceAmount() : BigDecimal.ZERO;
                String currency = (price.getPpriceCurr() != null) ? price.getPpriceCurr() : "KRW";

                // 3) 다음 주기 기간 (pmNextBil부터 시작)
                LocalDateTime start = (pm.getPmNextBil() != null) ? pm.getPmNextBil() : LocalDateTime.now();
                LocalDateTime end = start.plusMonths(months);

                // 4) 인보이스 생성 (★ 인보이스 금액/통화가 진실의 근원)
                PlanInvoiceEntity inv = PlanInvoiceEntity.builder()
                        .planMember(pm)
                        .piStart(start)
                        .piEnd(end)
                        .piAmount(amount)
                        .piCurr(currency)
                        .piStat(PlanEnums.PiStatus.PENDING)
                        .build();
                invoiceRepo.save(inv);

                // 5) 포트원 스케줄 등록 (★ 반드시 인보이스 금액/통화 사용)
                //    - orderId는 "inv{piId}" 형태로: 웹훅에서 매핑이 깔끔해짐
                String orderId = "inv" + inv.getPiId();
                // 실행시각: 인보이스 시작 직후(예: +60초)
                Instant runAt = start.plusSeconds(60).atZone(ZoneId.systemDefault()).toInstant();

                portone.scheduleByBillingKey(
                        orderId,
                        pm.getPayment().getPayKey(),
                        inv.getPiAmount().longValueExact(),  // ★ 인보이스 금액 그대로
                        inv.getPiCurr(),                     // ★ 인보이스 통화 그대로
                        pm.getPayment().getPayCustomer(),
                        "[자동결제] " + (price.getPlan() != null ? price.getPlan().getPlanName() : "구독"),
                        runAt
                );

                log.info("[AutoRenew] 예약 완료: pmId={}, orderId={}, months={}, amount={}, start={}",
                        pm.getPmId(), orderId, months, inv.getPiAmount(), inv.getPiStart());

            } catch (Exception e) {
                log.error("[AutoRenew] pmId={} 처리 중 오류: {}", pm.getPmId(), e.getMessage(), e);
            }
        }
    }
}
