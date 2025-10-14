package com.dodam.plan.scheduler;

import com.dodam.plan.Entity.PlanInvoiceEntity;
import com.dodam.plan.Entity.PlanMember;
import com.dodam.plan.Entity.PlanPriceEntity;
import com.dodam.plan.enums.PlanEnums.PiStatus;
import com.dodam.plan.enums.PlanEnums.PmBillingMode;
import com.dodam.plan.enums.PlanEnums.PmStatus;
import com.dodam.plan.repository.PlanInvoiceRepository;
import com.dodam.plan.repository.PlanMemberRepository;
import com.dodam.plan.repository.PlanPriceRepository;
import com.dodam.plan.service.PlanPortoneClientService;
import com.dodam.plan.service.PlanSubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlanSubscriptionScheduler {

    private final PlanMemberRepository planMemberRepo;
    private final PlanInvoiceRepository invoiceRepo;
    private final PlanPriceRepository priceRepo;
    private final PlanSubscriptionService subscriptionService;
    private final PlanPortoneClientService portone;

    /**
     * 매일 새벽 03:00 — “내일까지 결제 예정” 대상에 대해
     * PENDING 인보이스를 보장하고, PG 스케줄(자동결제)을 걸어둡니다.
     */
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    @Transactional
    public void scheduleUpcomingRenewals() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime until = now.plusDays(1);

        List<PlanMember> targets =
                planMemberRepo.findByPmNextBilBeforeAndPmStatusAndCancelAtPeriodEndFalse(
                        until, PmStatus.ACTIVE);

        for (PlanMember pm : targets) {
            // 결제수단/빌링키 확인
            if (pm.getPayment() == null || !StringUtils.hasText(pm.getPayment().getPayKey())) {
                log.warn("[Renewal] skip pmId={} (no billingKey)", pm.getPmId());
                continue;
            }

            // 금액/통화 산출
            AmountCurr ac = resolveAmountCurrency(pm);
            if (ac == null) {
                log.warn("[Renewal] skip pmId={} (no price)", pm.getPmId());
                continue;
            }

            // PENDING 인보이스 보장
            PlanInvoiceEntity inv = ensurePendingInvoiceFor(pm, ac.amount(), ac.currency());

            // PG 스케줄 등록(다음 결제일 시각에 자동결제)
            Instant schedAt = pm.getPmNextBil().atZone(ZoneId.systemDefault()).toInstant();
            String orderId = "renew-" + inv.getPiId();
            try {
                portone.scheduleByBillingKey(
                        orderId,
                        pm.getPayment().getPayKey(),
                        ac.amount().longValueExact(),
                        ac.currency(),
                        pm.getPayment().getPayCustomer(),
                        "Dodam Subscription Renewal",
                        schedAt
                );
                log.info("[Renewal] scheduled orderId={}, pmId={}, at={}", orderId, pm.getPmId(), pm.getPmNextBil());
            } catch (Exception e) {
                log.error("[Renewal] schedule failed pmId={}, msg={}", pm.getPmId(), e.getMessage());
            }
        }
    }

    /**
     * 매일 새벽 04:00 — 해지 예약(CANCEL_SCHEDULED)이고 종료일이 지난 구독을 완전 해지.
     */
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    @Transactional
    public void finalizeCanceled() {
        LocalDateTime now = LocalDateTime.now();
        List<PlanMember> due =
                planMemberRepo.findByPmStatusAndCancelAtPeriodEndTrueAndPmTermEndBefore(
                        PmStatus.CANCEL_SCHEDULED, now);

        for (PlanMember pm : due) {
            try {
                subscriptionService.finalizeCancelIfDue(pm.getPmId());
                log.info("[FinalizeCancel] pmId={} -> CANCELED", pm.getPmId());
            } catch (Exception e) {
                log.error("[FinalizeCancel] failed pmId={}, msg={}", pm.getPmId(), e.getMessage());
            }
        }
    }

    /** 현재 pm의 가격/통화 계산 (pm.price 우선, 없으면 plan/terms 기준 조회) */
    private AmountCurr resolveAmountCurrency(PlanMember pm) {
        if (pm.getPrice() != null) {
            String curr = Optional.ofNullable(pm.getPrice().getPpriceCurr()).orElse("KRW");
            return new AmountCurr(pm.getPrice().getPpriceAmount(), curr);
        }
        if (pm.getPlan() == null || pm.getTerms() == null) return null;

        String mode = (pm.getPmBilMode() == PmBillingMode.MONTHLY) ? "MONTHLY" : "PREPAID_TERM";
        Optional<PlanPriceEntity> priceOpt = priceRepo
                .findFirstByPlan_PlanIdAndPterm_PtermIdAndPpriceBilModeAndPpriceActiveTrue(
                        pm.getPlan().getPlanId(), pm.getTerms().getPtermId(), mode)
                .or(() -> priceRepo.findBestPrice(pm.getPlan().getPlanId(), pm.getTerms().getPtermId(), mode));

        if (priceOpt.isEmpty()) return null;
        PlanPriceEntity price = priceOpt.get();
        String curr = Optional.ofNullable(price.getPpriceCurr()).orElse("KRW");
        return new AmountCurr(price.getPpriceAmount(), curr);
    }

    /** 동일 금액/통화의 PENDING 인보이스가 최근 구간에 있으면 재사용, 없으면 생성 */
    private PlanInvoiceEntity ensurePendingInvoiceFor(PlanMember pm, BigDecimal amount, String currency) {
        String mid = pm.getMember().getMid();
        LocalDateTime now = LocalDateTime.now();
        return invoiceRepo.findRecentPendingSameAmount(
                        mid, PiStatus.PENDING, amount, currency, now.minusDays(3), now.plusDays(3))
                .orElseGet(() -> {
                    // 다음 주기에 맞춰 기간 설정
                    LocalDateTime start = pm.getPmTermEnd();
                    LocalDateTime end = start.plusMonths(pm.getPmCycle() != null ? pm.getPmCycle() : 1);

                    PlanInvoiceEntity inv = PlanInvoiceEntity.builder()
                            .planMember(pm)
                            .piStart(start)
                            .piEnd(end)
                            .piAmount(amount)
                            .piCurr(currency)
                            .piStat(PiStatus.PENDING)
                            .build();
                    return invoiceRepo.save(inv);
                });
    }

    private record AmountCurr(BigDecimal amount, String currency) {}
}
