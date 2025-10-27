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

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    @Transactional
    public void scheduleUpcomingRenewals() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime until = now.plusDays(1);

        List<PlanMember> targets =
                planMemberRepo.findByPmNextBilBeforeAndPmStatusAndCancelAtPeriodEndFalse(
                        until, PmStatus.ACTIVE);

        for (PlanMember pm : targets) {
            if (pm.getPayment() == null || !StringUtils.hasText(pm.getPayment().getPayKey())) {
                log.warn("[Renewal] skip pmId={} (no billingKey)", pm.getPmId());
                continue;
            }

            AmountCurr ac = resolveAmountCurrency(pm);
            if (ac == null) {
                log.warn("[Renewal] skip pmId={} (no price)", pm.getPmId());
                continue;
            }

            PlanInvoiceEntity inv = ensurePendingInvoiceFor(pm, ac.amount(), ac.currency());

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

    /** 가격/통화 계산 */
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
                    int cycle = Optional.ofNullable(pm.getPmCycle()).orElse(1);

                    // ✅ 기간 시작 계산 (절대 NULL 안 나옴)
                    LocalDateTime start = Optional.ofNullable(pm.getPmTermEnd())
                            .map(d -> d.plusSeconds(1)) // 직전 구간 종료 바로 다음 초
                            .orElseGet(() -> {
                                LocalDateTime next = Optional.ofNullable(pm.getPmNextBil())
                                        .orElse(LocalDateTime.now());
                                return next.minusMonths(cycle);
                            });

                    // ✅ 종료 = 시작 + cycle개월 - 1초
                    LocalDateTime end = start.plusMonths(cycle).minusSeconds(1);

                    PlanInvoiceEntity inv = PlanInvoiceEntity.builder()
                            .planMember(pm)
                            .piStart(start)
                            .piEnd(end)               // ✅ 핵심
                            .piAmount(amount)
                            .piCurr(currency)
                            .piStat(PiStatus.PENDING)
                            .build();

                    return invoiceRepo.save(inv);
                });
    }

    private record AmountCurr(BigDecimal amount, String currency) {}
}
