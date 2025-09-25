// src/main/java/com/dodam/plan/controller/PlanSubscriptionController.java
package com.dodam.plan.controller;

import com.dodam.member.entity.MemberEntity;
import com.dodam.member.repository.MemberRepository;
import com.dodam.plan.Entity.*;
import com.dodam.plan.dto.PlanSubscriptionStartReq;
import com.dodam.plan.enums.PlanEnums.PiStatus;
import com.dodam.plan.enums.PlanEnums.PmBillingMode;
import com.dodam.plan.enums.PlanEnums.PmStatus;
import com.dodam.plan.repository.*;
import com.dodam.plan.service.PlanSubscriptionService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/subscriptions")
public class PlanSubscriptionController {

    private final PlanInvoiceRepository invoiceRepo;
    private final PlanMemberRepository planMemberRepo;
    private final PlansRepository plansRepo;
    private final PlanTermsRepository termsRepo;
    private final PlanPriceRepository priceRepo;
    private final PlanPaymentRepository paymentRepo;
    private final MemberRepository memberRepo;

    private final PlanSubscriptionService subscriptionService;

    /** 인보이스만 만들고 끝(기존 로직 유지) */
    @PostMapping(value = "/start", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<?> start(@RequestBody PlanSubscriptionStartReq req, HttpSession session) {
        final String mid = (String) session.getAttribute("sid");
        if (!StringUtils.hasText(mid)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "LOGIN_REQUIRED"));
        }

        // 0) 파라미터 정규화
        final int months = (req.getMonths() != null && req.getMonths() > 0) ? req.getMonths() : 1;
        final String planCode = (req.getPlanCode() != null) ? req.getPlanCode().trim() : null;
        if (!StringUtils.hasText(planCode)) {
            return ResponseEntity.badRequest().body(Map.of("error", "MISSING_PLAN_CODE"));
        }
 
        // 1) 회원/결제수단/플랜/약정 조회
        MemberEntity member = memberRepo.findByMid(mid)
                .orElseThrow(() -> new IllegalStateException("회원 정보를 찾을 수 없습니다. mid=" + mid));

        PlanPaymentEntity payment = paymentRepo.findTopByMidOrderByPayIdDesc(mid)
                .orElseThrow(() -> new IllegalStateException("결제수단이 없습니다. 먼저 카드(빌링키)를 등록하세요."));

        PlansEntity plan = plansRepo.findByPlanCodeIgnoreCase(planCode)
                .orElseGet(() -> plansRepo.findByPlanCodeEqualsIgnoreCase(planCode)
                        .orElseThrow(() -> new IllegalStateException("플랜 코드가 유효하지 않습니다. planCode=" + planCode)));

        PlanTermsEntity terms = termsRepo.findByPtermMonth(months)
                .orElseThrow(() -> new IllegalStateException("해당 개월 약정이 없습니다. months=" + months));

        // 2) 결제 모드 결정
        final String mode = (months == 1) ? "MONTHLY" : "PREPAID_TERM";

        // 3) 가격 조회
        PlanPriceEntity price = priceRepo
                .findFirstByPlan_PlanIdAndPterm_PtermIdAndPpriceBilModeAndPpriceActiveTrue(
                        plan.getPlanId(), terms.getPtermId(), mode)
                .or(() -> priceRepo.findBestPrice(plan.getPlanId(), terms.getPtermId(), mode))
                .orElseThrow(() -> new IllegalStateException(
                        "가격 정보가 없습니다. plan=" + planCode + ", months=" + months));

        final BigDecimal amount = price.getPpriceAmount();
        final String currency = StringUtils.hasText(price.getPpriceCurr()) ? price.getPpriceCurr() : "KRW";

        // 4) PlanMember upsert
        PlanMember pm = planMemberRepo.findByMember_Mid(mid).orElse(null);
        if (pm == null) {
            LocalDateTime now = LocalDateTime.now();
            pm = PlanMember.builder()
                    .member(member)
                    .payment(payment)
                    .plan(plan)
                    .terms(terms)
                    .price(price)
                    .pmStat(PmStatus.ACTIVE)
                    .pmBilMode(PmBillingMode.valueOf(mode))
                    .pmStart(now)
                    .pmTermStart(now)
                    .pmTermEnd(now.plusMonths(months))
                    .pmNextBil(now.plusMonths(months))
                    .pmCycle(months)
                    .pmCancelCheck(false)
                    .build();
            pm = planMemberRepo.save(pm);
            log.info("[subscriptions/start] PlanMember created mid={}, pmId={}", mid, pm.getPmId());
        }

        // 5) 멱등 체크 (최근 10분 내 동일 금액/통화 PENDING)
        final LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        final LocalDateTime from = now.minusMinutes(10);
        var recentOpt = invoiceRepo.findRecentPendingSameAmount(mid, PiStatus.PENDING, amount, currency, from, now);
        if (recentOpt.isPresent()) {
            var inv = recentOpt.get();
            log.info("[subscriptions/start] ALREADY_PENDING mid={}, invoiceId={}", mid, inv.getPiId());

            Map<String, Object> body = new HashMap<>();
            body.put("message", "ALREADY_PENDING");
            body.put("invoiceId", inv.getPiId());
            body.put("amount", inv.getPiAmount());
            body.put("currency", inv.getPiCurr());
            body.put("start", inv.getPiStart());
            body.put("end", inv.getPiEnd());
            return ResponseEntity.ok(body);
        }

        // 6) 신규 인보이스 생성
        var inv = PlanInvoiceEntity.builder()
                .planMember(pm)
                .piStart(now)
                .piEnd(now.plusMonths(months))
                .piAmount(amount)
                .piCurr(currency)
                .piStat(PiStatus.PENDING)
                .build();
        invoiceRepo.save(inv);

        log.info("[subscriptions/start] PENDING_CREATED mid={}, invoiceId={}", mid, inv.getPiId());

        Map<String, Object> body = new HashMap<>();
        body.put("message", "PENDING_CREATED");
        body.put("invoiceId", inv.getPiId());
        body.put("amount", inv.getPiAmount());
        body.put("currency", inv.getPiCurr());
        body.put("start", inv.getPiStart());
        body.put("end", inv.getPiEnd());
        return ResponseEntity.ok(body);
    }

    /**
     * ✅ (신규) 인보이스를 바로 빌링키로 결제 트리거하고, 폴링으로 확정까지 처리
     *  - 로컬/샌드박스 환경에서 웹훅이 localhost로 못 들어오는 문제를 해결
     */
    @PostMapping(value = "/charge-and-confirm", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Transactional
    public ResponseEntity<?> chargeAndConfirm(@RequestBody PlanSubscriptionStartReq req, HttpSession session) {
        final String mid = (String) session.getAttribute("sid");
        if (!StringUtils.hasText(mid)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "LOGIN_REQUIRED"));
        }

        try {
            // 1) 인보이스 생성 or 재사용 (네 기존 /start 로직을 내부 메서드로 뽑거나, 여기서 재사용)
            var result = subscriptionService.chargeAndConfirm(mid, req);
            return ResponseEntity.ok(result); // result 안에 invoiceId, paymentId, status, receiptUrl 등
        } catch (IllegalStateException ex) {
            log.warn("[charge-and-confirm] {}", ex.getMessage(), ex);
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            log.error("[charge-and-confirm] unexpected", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "INTERNAL_SERVER_ERROR"));
        }
    }
}
