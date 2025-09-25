// src/main/java/com/dodam/plan/controller/PlanImmediateConfirmController.java
package com.dodam.plan.controller;

import com.dodam.plan.Entity.PlanInvoiceEntity;
import com.dodam.plan.Entity.PlanPaymentEntity;
import com.dodam.plan.enums.PlanEnums.PiStatus;
import com.dodam.plan.repository.PlanInvoiceRepository;
import com.dodam.plan.repository.PlanPaymentRepository;
import com.dodam.plan.repository.PlanPriceRepository;
import com.dodam.plan.repository.PlanTermsRepository;
import com.dodam.plan.service.PlanBillingService;
import com.dodam.plan.service.PlanPaymentGatewayService;
import com.dodam.plan.service.PlanSubscriptionService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/payments")
@ConditionalOnProperty(value = "payments.confirm.immediate.enabled", havingValue = "true", matchIfMissing = false)
public class PlanImmediateConfirmController {

	private final PlanPaymentRepository paymentRepo;
	private final PlanInvoiceRepository invoiceRepo;
	private final PlanPaymentGatewayService pgSvc;
	private final PlanBillingService billingSvc;

	// 기간 계산용
	private final PlanPriceRepository priceRepo;
	private final PlanTermsRepository termsRepo;
	private final PlanSubscriptionService subscriptionService;

	@PostMapping(value = "/confirm", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> confirm(@RequestBody Map<String, Object> body, HttpSession session) {
		String mid = (String) session.getAttribute("sid");
		if (!StringUtils.hasText(mid)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "LOGIN_REQUIRED"));
		}

		Long invoiceId = parseLong(body.get("invoiceId"));
		if (invoiceId == null)
			return ResponseEntity.badRequest().body(Map.of("error", "MISSING_INVOICE_ID"));

		Optional<PlanInvoiceEntity> optInv = invoiceRepo.findById(invoiceId);
		if (optInv.isEmpty())
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "INVOICE_NOT_FOUND"));
		PlanInvoiceEntity inv = optInv.get();

		// 최신 카드(빌링키)
		PlanPaymentEntity card = paymentRepo.findTop1ByMidOrderByPayIdDesc(mid).orElseGet(() -> {
			List<PlanPaymentEntity> list = paymentRepo.findByMid(mid);
			if (list == null || list.isEmpty())
				return null;
			return list
					.stream().filter(Objects::nonNull).sorted(Comparator
							.comparing(PlanPaymentEntity::getPayId, Comparator.nullsLast(Long::compareTo)).reversed())
					.findFirst().orElse(null);
		});
		if (card == null || !StringUtils.hasText(card.getPayKey())) {
			return ResponseEntity.status(HttpStatus.PRECONDITION_REQUIRED).body(Map.of("error", "NO_BILLING_KEY"));
		}

		// 🔸 개월수 결정: priceId → termId → months
		Integer months = resolveMonths(body, inv);
		if (months == null)
			months = 1;

		// 결제 수행
		String paymentId = "inv" + inv.getPiId() + "-ts" + System.currentTimeMillis();
		long amount = safeAmountToLong(inv.getPiAmount(), inv.getPiCurr());
		var res = pgSvc.payByBillingKey(paymentId, card.getPayKey(), amount, card.getPayCustomer());
		boolean ok = res.success();

		// 시도 기록
		billingSvc.recordAttempt(inv.getPiId(), ok, res.failReason(), res.paymentId(), // ✅ providerId 있으면 무조건 pattUid
																						// 저장
				res.receiptUrl(), res.rawJson());

		if (ok) {
			inv.setPiStat(PiStatus.PAID);
			inv.setPiPaid(LocalDateTime.now());
			invoiceRepo.save(inv);

			// ✅ 구독 기간 부여/연장
			subscriptionService.activateInvoice(inv, months);

			return ResponseEntity.ok(Map.of("result", "PAID", "paymentId", res.paymentId(), "receiptUrl",
					res.receiptUrl(), "months", months, "start", inv.getPiStart(), "end", inv.getPiEnd()));
		} else {
			String st = res.status() == null ? "UNKNOWN" : res.status().trim().toUpperCase();
			if ("UNKNOWN".equals(st) || "ERROR".equals(st) || "PENDING".equals(st)) {
				inv.setPiStat(PiStatus.PENDING);
				invoiceRepo.save(inv);
				return ResponseEntity.accepted()
						.body(Map.of("result", "PENDING", "paymentId", res.paymentId(), "months", months));
			}
			inv.setPiStat(PiStatus.FAILED);
			invoiceRepo.save(inv);
			return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("result", "FAIL", "paymentId",
					res.paymentId(), "reason", Optional.ofNullable(res.failReason()).orElse(st)));
		}
	}

	@GetMapping("/confirm")
	public ResponseEntity<?> rejectGetConfirm() {
		return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
				.body(Map.of("message", "Use POST /payments/confirm"));
	}

	private Integer resolveMonths(Map<String, Object> body, PlanInvoiceEntity inv) {
		Long priceId = parseLong(body.get("priceId"));
		Long termId = parseLong(body.get("termId"));

		try {
			if (priceId != null) {
				Long tid = priceRepo.findTermIdByPriceId(priceId);
				if (tid != null) {
					Integer m = termsRepo.findMonthsByTermId(tid);
					if (m != null)
						return m;
				}
			}
			if (termId != null) {
				Integer m = termsRepo.findMonthsByTermId(termId);
				if (m != null)
					return m;
			}
		} catch (Exception ignore) {
		}

		// (선택) 인보이스가 term/price를 이미 품고 있으면 여기서 꺼내 계산 가능
		return null;
	}

	private long safeAmountToLong(BigDecimal bd, String currency) {
		if (bd == null)
			return 0L;
		return bd.setScale(0, java.math.RoundingMode.HALF_UP).longValueExact();
	}

	private Long parseLong(Object v) {
		try {
			if (v instanceof Number n)
				return n.longValue();
			if (v != null && !v.toString().isBlank())
				return Long.parseLong(v.toString());
		} catch (Exception ignore) {
		}
		return null;
	}
}
