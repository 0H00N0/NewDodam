package com.dodam.plan.controller;

import com.dodam.plan.Entity.PlanInvoiceEntity;
import com.dodam.plan.repository.PlanInvoiceRepository;
import com.dodam.plan.repository.PlanMemberRepository;
import com.dodam.plan.service.PlanPortoneClientService;
import com.dodam.plan.enums.PlanEnums.PiStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/test")
public class SubscriptionTestController {

  private final PlanMemberRepository planMemberRepo;
  private final PlanInvoiceRepository invoiceRepo;
  private final PlanPortoneClientService portone;

  @RequestMapping(value="/auto-renew", method={RequestMethod.GET, RequestMethod.POST})
  public String testAutoRenew(@RequestParam("pmId") Long pmId) {
    var pm = planMemberRepo.findById(pmId)
        .orElseThrow(() -> new IllegalArgumentException("PlanMember not found: " + pmId));
    if (pm.getPayment()==null || pm.getPayment().getPayKey()==null) {
      throw new IllegalStateException("빌링키가 없습니다. 카드 등록 먼저 진행하세요.");
    }

    BigDecimal amount = (pm.getPrice()!=null ? pm.getPrice().getPpriceAmount() : new BigDecimal("100"));
    String currency = (pm.getPrice()!=null && pm.getPrice().getPpriceCurr()!=null) ? pm.getPrice().getPpriceCurr() : "KRW";

    var inv = PlanInvoiceEntity.builder()
        .planMember(pm)
        .piStart(LocalDateTime.now())
        .piEnd(LocalDateTime.now().plusMonths(1))
        .piAmount(amount)
        .piCurr(currency)
        .piStat(PiStatus.PENDING)
        .build();
    invoiceRepo.save(inv);

    String orderId = "test-" + inv.getPiId();
    Instant at = Instant.now().plusSeconds(60);

    portone.scheduleByBillingKey(
        orderId,
        pm.getPayment().getPayKey(),
        amount.longValueExact(),
        currency,
        pm.getPayment().getPayCustomer(),
        "[테스트] Dodam Subscription Auto-Renew",
        at
    );

    log.info("[TEST] 예약 성공 pmId={}, invoiceId={}, 실행시각={}",
        pmId, inv.getPiId(), LocalDateTime.ofInstant(at, ZoneId.systemDefault()));

    return "✅ 1분 후 자동 결제 예약: invoiceId=" + inv.getPiId();
  }
}
