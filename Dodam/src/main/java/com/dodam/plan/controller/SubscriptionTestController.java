package com.dodam.plan.controller;

import com.dodam.plan.Entity.PlanInvoiceEntity;
import com.dodam.plan.Entity.PlanMember;
import com.dodam.plan.Entity.PlanPriceEntity;
import com.dodam.plan.Entity.PlanTermsEntity;
import com.dodam.plan.enums.PlanEnums.PiStatus;
import com.dodam.plan.repository.PlanInvoiceRepository;
import com.dodam.plan.repository.PlanMemberRepository;
import com.dodam.plan.service.PlanPortoneClientService;
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

	// http://도메인/test/auto-renew?pmId=번호&forceNow=1
	@RequestMapping(value = "/auto-renew", method = { RequestMethod.GET, RequestMethod.POST })
	public String testAutoRenew(@RequestParam("pmId") Long pmId,
			@RequestParam(value = "forceNow", required = false, defaultValue = "0") int forceNowSecOffset) {
		var pm = planMemberRepo.findById(pmId)
				.orElseThrow(() -> new IllegalArgumentException("PlanMember not found: " + pmId));
		if (pm.getPayment() == null || pm.getPayment().getPayKey() == null) {
			throw new IllegalStateException("빌링키가 없습니다. 카드 등록 먼저 진행하세요.");
		}

		// next* 있으면 변경예약 우선, 없으면 현재 플랜/가격 사용
		var price = (pm.getNextPrice() != null) ? pm.getNextPrice() : pm.getPrice();
		if (price == null)
			throw new IllegalStateException("가격 정보가 없습니다.");

		int months = (pm.getNextTerms() != null && pm.getNextTerms().getPtermMonth() != null)
				? Math.max(1, pm.getNextTerms().getPtermMonth())
				: (price.getPterm() != null && price.getPterm().getPtermMonth() != null)
						? Math.max(1, price.getPterm().getPtermMonth())
						: 1;

		var amount = price.getPpriceAmount();
		var currency = (price.getPpriceCurr() != null) ? price.getPpriceCurr() : "KRW";

		// ✅ forceNow=1 이면 지금 기준, 아니면 pmNextBil 기준
		LocalDateTime baseStart = (forceNowSecOffset != 0) ? LocalDateTime.now()
				: (pm.getPmNextBil() != null ? pm.getPmNextBil() : LocalDateTime.now());

		LocalDateTime start = baseStart;
		LocalDateTime end = start.plusMonths(months);

		var inv = PlanInvoiceEntity.builder().planMember(pm).piStart(start).piEnd(end).piAmount(amount).piCurr(currency)
				.piStat(PiStatus.PENDING).build();
		invoiceRepo.save(inv);

		// 📌 웹훅 매핑을 위해 주문ID는 inv{piId}
		String orderId = "inv" + inv.getPiId();

		// ✅ 실제 실행 시각: forceNow면 "지금+60초", 아니면 "start+60초"
		Instant at = ((forceNowSecOffset != 0) ? Instant.now() : start.atZone(ZoneId.systemDefault()).toInstant())
				.plusSeconds(60);

		portone.scheduleByBillingKey(orderId, pm.getPayment().getPayKey(), inv.getPiAmount().longValueExact(),
				inv.getPiCurr(), pm.getPayment().getPayCustomer(),
				(pm.getNextPrice() != null ? "[변경테스트] " : "[재구독테스트] ") + price.getPlan().getPlanName(), at);

		log.info("[TEST] 예약 성공 pmId={}, invoiceId={}, months={}, amount={}, 실행시각={}", pmId, inv.getPiId(), months,
				amount, LocalDateTime.ofInstant(at, ZoneId.systemDefault()));

		return "✅ 예약됨: orderId=" + orderId + ", invoiceId=" + inv.getPiId() + ", months=" + months + ", amount="
				+ amount + ", runAt=" + LocalDateTime.ofInstant(at, ZoneId.systemDefault());
	}
}
