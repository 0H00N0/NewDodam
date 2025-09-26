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

        // 1) 회원 조회
        MemberEntity member = memberRepo.findByMid(mid)
                .orElseThrow(() -> new IllegalStateException("회원 정보를 찾을 수 없습니다. mid=" + mid));

        // ✅ 결제수단 선택 (payId → billingKey → fallback 최신순)
        PlanPaymentEntity payment;
        if (req.getPayId() != null) {
            payment = paymentRepo.findById(req.getPayId())
                    .orElseThrow(() -> new IllegalStateException("선택한 결제수단을 찾을 수 없습니다. payId=" + req.getPayId()));
            if (!payment.getMid().equals(mid)) {
                throw new IllegalStateException("본인 소유 카드가 아닙니다. mid=" + mid + ", payId=" + req.getPayId());
            }
        } else if (StringUtils.hasText(req.getBillingKey())) {
            payment = paymentRepo.findByPayKey(req.getBillingKey())
                    .orElseThrow(() -> new IllegalStateException("해당 빌링키 결제수단이 없습니다. billingKey=" + req.getBillingKey()));
            if (!payment.getMid().equals(mid)) {
                throw new IllegalStateException("본인 소유 빌링키가 아닙니다. mid=" + mid + ", billingKey=" + req.getBillingKey());
            }
        } else {
            payment = paymentRepo.findTopByMidOrderByPayIdDesc(mid)
                    .orElseThrow(() -> new IllegalStateException("결제수단이 없습니다. 먼저 카드(빌링키)를 등록하세요."));
        }

        // 2) 플랜/약정 조회
        PlansEntity plan = plansRepo.findByPlanCodeIgnoreCase(planCode)
                .orElseGet(() -> plansRepo.findByPlanCodeEqualsIgnoreCase(planCode)
                        .orElseThrow(() -> new IllegalStateException("플랜 코드가 유효하지 않습니다. planCode=" + planCode)));

        PlanTermsEntity terms = termsRepo.findByPtermMonth(months)
                .orElseThrow(() -> new IllegalStateException("해당 개월 약정이 없습니다. months=" + months));

        // 3) 결제 모드 결정
        final String mode = (months == 1) ? "MONTHLY" : "PREPAID_TERM";

        // 4) 가격 조회
        PlanPriceEntity price = priceRepo
                .findFirstByPlan_PlanIdAndPterm_PtermIdAndPpriceBilModeAndPpriceActiveTrue(
                        plan.getPlanId(), terms.getPtermId(), mode)
                .or(() -> priceRepo.findBestPrice(plan.getPlanId(), terms.getPtermId(), mode))
                .orElseThrow(() -> new IllegalStateException(
                        "가격 정보가 없습니다. plan=" + planCode + ", months=" + months));

        final BigDecimal amount = price.getPpriceAmount();
        final String currency = StringUtils.hasText(price.getPpriceCurr()) ? price.getPpriceCurr() : "KRW";

        // 5) PlanMember upsert
        PlanMember pm = planMemberRepo.findByMember_Mid(mid).orElse(null);
        if (pm == null) {
            LocalDateTime now = LocalDateTime.now();
            pm = PlanMember.builder()
                    .member(member)
                    .payment(payment) // 최초 생성 시 연결
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
            log.info("[subscriptions/start] PlanMember created mid={}, pmId={}, payId={}", mid, pm.getPmId(), payment.getPayId());
        } else {
            // ✅ 기존 PlanMember가 있는 경우, 이번 결제에 선택한 카드로 갱신 (핵심 수정)
            boolean updated = false;
            if (pm.getPayment() == null || !pm.getPayment().getPayId().equals(payment.getPayId())) {
                pm.setPayment(payment);
                updated = true;
                log.info("[subscriptions/start] PlanMember payment updated mid={}, pmId={}, payId={}", mid, pm.getPmId(), payment.getPayId());
            }
            // (선택) 플랜/약정/가격도 요청값과 다르면 동기화
            if (!pm.getPlan().getPlanId().equals(plan.getPlanId())) { pm.setPlan(plan); updated = true; }
            if (!pm.getTerms().getPtermId().equals(terms.getPtermId())) { pm.setTerms(terms); updated = true; }
            if (!pm.getPrice().getPpriceId().equals(price.getPpriceId())) { pm.setPrice(price); updated = true; }
            if (updated) {
                planMemberRepo.save(pm);
            }
        }

        // 6) 멱등 체크 (최근 10분 내 동일 금액/통화 PENDING)
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
            body.put("payId", pm.getPayment() != null ? pm.getPayment().getPayId() : null); // 디버깅용 반환
            return ResponseEntity.ok(body);
        }

        // 7) 신규 인보이스 생성
        var inv = PlanInvoiceEntity.builder()
                .planMember(pm)
                .piStart(now)
                .piEnd(now.plusMonths(months))
                .piAmount(amount)
                .piCurr(currency)
                .piStat(PiStatus.PENDING)
                .build();
        invoiceRepo.save(inv);

        log.info("[subscriptions/start] PENDING_CREATED mid={}, invoiceId={}, payId={}", mid, inv.getPiId(), pm.getPayment() != null ? pm.getPayment().getPayId() : null);

        Map<String, Object> body = new HashMap<>();
        body.put("message", "PENDING_CREATED");
        body.put("invoiceId", inv.getPiId());
        body.put("amount", inv.getPiAmount());
        body.put("currency", inv.getPiCurr());
        body.put("start", inv.getPiStart());
        body.put("end", inv.getPiEnd());
        body.put("payId", pm.getPayment() != null ? pm.getPayment().getPayId() : null); // 디버깅용 반환
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
            var result = subscriptionService.chargeAndConfirm(mid, req);
            return ResponseEntity.ok(result);
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
