package com.dodam.plan.service;

import com.dodam.member.entity.MemberEntity;
import com.dodam.member.repository.MemberRepository;
import com.dodam.plan.Entity.*;
import com.dodam.plan.dto.PlanSubscriptionStartReq;
import com.dodam.plan.enums.PlanEnums.PiStatus;
import com.dodam.plan.enums.PlanEnums.PmBillingMode;
import com.dodam.plan.enums.PlanEnums.PmStatus;
import com.dodam.plan.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanSubscriptionServiceImpl implements PlanSubscriptionService {

    private final PlanInvoiceRepository invoiceRepo;
    private final PlanMemberRepository planMemberRepo;
    private final PlansRepository plansRepo;
    private final PlanTermsRepository termsRepo;
    private final PlanPriceRepository priceRepo;
    private final PlanPaymentRepository paymentRepo;
    private final MemberRepository memberRepo;

    private final PlanPortoneClientService portoneClient;

    private static final Duration TIMEOUT_DEFAULT = Duration.ofSeconds(90);
    private static final Duration POLL_INTERVAL   = Duration.ofSeconds(1);
    private final ObjectMapper om = new ObjectMapper();

    /* =========================
     *  결제 확정 → 구독 반영
     * ========================= */
    @Override
    @Transactional
    public void activateInvoice(PlanInvoiceEntity invoice, int months) {
        PlanMember pm = invoice.getPlanMember();
        if (pm == null) throw new IllegalStateException("PlanMember가 없는 인보이스입니다. piId=" + invoice.getPiId());

        LocalDateTime now = LocalDateTime.now();

        // ✅ 다음 주기 변경 예약이 있다면, 이번 활성화 시점에 실제 값으로 스왑
        if (pm.getNextPlan() != null)  pm.setPlan(pm.getNextPlan());
        if (pm.getNextTerms() != null) pm.setTerms(pm.getNextTerms());
        if (pm.getNextPrice() != null) pm.setPrice(pm.getNextPrice());
        pm.clearPendingChange(); // 위에서 만든 유틸(없으면 각각 null 처리)

        // 상태 보정
        if (pm.getPmStatus() == null || pm.getPmStatus() == PmStatus.CANCELED) {
            pm.setPmStatus(PmStatus.ACTIVE);
            pm.setCancelAtPeriodEnd(false);
            pm.setCanceledAt(null);
        }
        pm.setPmBilMode(months == 1 ? PmBillingMode.MONTHLY : PmBillingMode.PREPAID_TERM);

        // 현재 주기가 남아있으면 연장, 아니면 지금부터 시작(네 기존 코드 유지)
        LocalDateTime termStart = (pm.getPmTermEnd() != null && pm.getPmTermEnd().isAfter(now))
                ? pm.getPmTermEnd()
                : now;

        pm.setPmTermStart(termStart);
        pm.setPmTermEnd(termStart.plusMonths(months));

        // ✅ 다음 결제일은 항상 새 주기의 끝으로 갱신
        pm.setPmNextBil(pm.getPmTermEnd());
        pm.setPmCycle(months);
        planMemberRepo.save(pm);

        invoice.setPiStat(PiStatus.PAID);
        invoice.setPiPaid(LocalDateTime.now());
        invoiceRepo.save(invoice);

        log.info("[Subscription] invoice activated. pmId={}, term={}~{}, nextBil={}",
                pm.getPmId(), pm.getPmTermStart(), pm.getPmTermEnd(), pm.getPmNextBil());
    }

    /* =========================
     *  즉시 결제 + 폴링 확정
     * ========================= */
    @Override
    @Transactional
    public Map<String, Object> chargeByBillingKeyAndConfirm(Long invoiceId, String mid, int termMonths) {
        if (termMonths <= 0) termMonths = 1;

        PlanInvoiceEntity invoice = invoiceRepo.findById(invoiceId)
                .orElseThrow(() -> new IllegalStateException("인보이스가 존재하지 않습니다. invoiceId=" + invoiceId));

        if (invoice.getPiStat() != PiStatus.PENDING) {
            throw new IllegalStateException("결제 가능한 상태가 아닙니다. 현재=" + invoice.getPiStat());
        }

        memberRepo.findByMid(mid)
                .orElseThrow(() -> new IllegalStateException("회원 정보를 찾을 수 없습니다. mid=" + mid));

        PlanMember pm = invoice.getPlanMember();
        if (pm == null) throw new IllegalStateException("인보이스에 연결된 PlanMember가 없습니다.");

        // 🔒 기간말 해지 예약이고 이미 주기가 끝났으면 결제 차단
        if (Boolean.TRUE.equals(pm.isCancelAtPeriodEnd())
                && pm.getPmTermEnd() != null
                && !LocalDateTime.now().isBefore(pm.getPmTermEnd())) {
            throw new IllegalStateException("해지 예약된 구독입니다. 주기 종료 이후에는 재구독이 필요합니다.");
        }

        PlanPaymentEntity payment = pm.getPayment();
        if (payment == null || !StringUtils.hasText(payment.getPayKey())) {
            throw new IllegalStateException("결제수단이 없습니다. 먼저 빌링키를 등록하세요.");
        }

        String billingKey = payment.getPayKey();
        BigDecimal amount = invoice.getPiAmount();
        String currency = StringUtils.hasText(invoice.getPiCurr()) ? invoice.getPiCurr() : "KRW";
        long amountLong = amount.longValueExact();

        String paymentId = "inv" + invoice.getPiId() + "-ts" + System.currentTimeMillis();
        String orderName = "Dodam Subscription";

        portoneClient.scheduleByBillingKey(
                paymentId,
                billingKey,
                amountLong,
                currency,
                payment.getPayCustomer(),
                orderName,
                Instant.now().plusSeconds(3)
        );
        log.info("[PortOne] scheduleByBillingKey requested. paymentId={}", paymentId);

        JsonNode result = pollUntilPaid(paymentId, TIMEOUT_DEFAULT);
        String status = result.path("status").asText("");

        Map<String, Object> resp = new HashMap<>();
        resp.put("invoiceId", invoiceId);
        resp.put("paymentId", paymentId);
        resp.put("status", status);

        if (isPaid(status)) {
            String providerPaymentId = firstNonBlank(
                    result.path("id").asText(null),
                    result.at("/payment/id").asText(null),
                    result.at("/items/0/id").asText(null)
            );

            invoice.setPiStat(PiStatus.PAID);
            invoice.setPiPaid(LocalDateTime.now());
            if (StringUtils.hasText(providerPaymentId)) {
                invoice.setPiUid(providerPaymentId);
            }
            invoiceRepo.save(invoice);

            activateInvoice(invoice, termMonths);

            try {
                updatePaymentCardMetaIfPresent(payment, result);
                JsonNode byOrder = portoneClient.getPaymentByOrderId(paymentId);
                updatePaymentCardMetaIfPresent(payment, byOrder);
            } catch (Exception e) {
                log.debug("[PaymentMeta] fetch/update skipped: {}", e.toString());
            }

            String receiptUrl = result.path("receiptUrl").asText(null);
            resp.put("receiptUrl", receiptUrl);
            log.info("[Subscription] payment {} confirmed by polling", paymentId);

        } else if (isFailed(status)) {
            invoice.setPiStat(PiStatus.FAILED);
            invoiceRepo.save(invoice);
            resp.put("error", result.toString());
            log.warn("[Subscription] payment {} failed: {}", paymentId, result.toString());

        } else {
            resp.put("info", result.toString());
            log.warn("[Subscription] payment {} not confirmed in time: {}", paymentId, result.toString());
        }

        return resp;
    }

    private JsonNode pollUntilPaid(String orderId, Duration timeout) {
        Instant end = Instant.now().plus(timeout != null ? timeout : TIMEOUT_DEFAULT);
        Exception lastError = null;
        JsonNode lastSeen = null;

        try { Thread.sleep(700); } catch (InterruptedException ignored) {}

        while (Instant.now().isBefore(end)) {
            try {
                JsonNode byOrder = portoneClient.getPaymentByOrderId(orderId);
                if (byOrder != null && !byOrder.isMissingNode()) {
                    JsonNode node = firstPaymentNode(byOrder);
                    lastSeen = node;

                    String s = pickStatus(node);
                    if (isTerminal(s)) return node;

                    String id1 = pick(node, "id");
                    if (id1 == null) id1 = pick(node.path("payment"), "id");
                    String tx  = pick(node, "transactionId");
                    if (tx == null) tx = pick(node.path("payment"), "transactionId");

                    String candidate = null;
                    if (id1 != null && !id1.isBlank() && !id1.startsWith("inv")) candidate = id1;
                    else if (tx != null && !tx.isBlank()) candidate = tx;

                    if (candidate != null) {
                        var lr = portoneClient.lookupPayment(candidate);
                        if (lr != null && lr.raw() != null) {
                            JsonNode j = safeJson(lr.raw());
                            JsonNode node2 = firstPaymentNode(j);
                            String s2 = pickStatus(node2);
                            if (isTerminal(s2)) return node2;
                            lastSeen = node2;
                        }
                    }
                }
            } catch (Exception e) {
                lastError = e;
                log.debug("[PortOne] polling error (ignored): {}", e.toString());
            }

            try { Thread.sleep(POLL_INTERVAL.toMillis()); }
            catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
        }

        ObjectNode timeoutNode = om.createObjectNode();
        timeoutNode.put("status", "TIMEOUT");
        timeoutNode.put("paymentId", orderId);
        if (lastSeen != null) timeoutNode.set("lastSeen", lastSeen);
        if (lastError != null) timeoutNode.put("lastError", lastError.toString());
        return timeoutNode;
    }

    /* =========================
     *  시작(생성) + 결제
     * ========================= */
    @Override
    @Transactional
    public Map<String, Object> chargeAndConfirm(String mid, PlanSubscriptionStartReq req) {
        int months = (req.getMonths() != null && req.getMonths() > 0) ? req.getMonths() : 1;
        String planCode = (req.getPlanCode() != null) ? req.getPlanCode().trim() : null;
        if (!StringUtils.hasText(planCode)) throw new IllegalStateException("MISSING_PLAN_CODE");

        MemberEntity member = memberRepo.findByMid(mid)
                .orElseThrow(() -> new IllegalStateException("회원 정보를 찾을 수 없습니다. mid=" + mid));

        // ✅ 사용자가 고른 결제수단을 정확히 집는다
        PlanPaymentEntity payment;
        if (req.getPayId() != null) {
            payment = paymentRepo.findById(req.getPayId())
                    .orElseThrow(() -> new IllegalStateException("선택한 결제수단이 존재하지 않습니다. payId=" + req.getPayId()));
        } else if (StringUtils.hasText(req.getBillingKey())) {
            payment = paymentRepo.findByPayKey(req.getBillingKey())
                    .orElseThrow(() -> new IllegalStateException("선택한 빌링키가 존재하지 않습니다."));
        } else {
            payment = paymentRepo.findTopByMidOrderByPayIdDesc(mid)
                    .orElseThrow(() -> new IllegalStateException("결제수단이 없습니다. 먼저 카드(빌링키)를 등록하세요."));
        }

        PlansEntity plan = plansRepo.findByPlanCodeIgnoreCase(planCode)
                .orElseGet(() -> plansRepo.findByPlanCodeEqualsIgnoreCase(planCode)
                        .orElseThrow(() -> new IllegalStateException("플랜 코드가 유효하지 않습니다. planCode=" + planCode)));

        PlanTermsEntity terms = termsRepo.findByPtermMonth(months)
                .orElseThrow(() -> new IllegalStateException("해당 개월 약정이 없습니다. months=" + months));

        final String mode = (months == 1) ? "MONTHLY" : "PREPAID_TERM";

        PlanPriceEntity price = priceRepo
                .findFirstByPlan_PlanIdAndPterm_PtermIdAndPpriceBilModeAndPpriceActiveTrue(
                        plan.getPlanId(), terms.getPtermId(), mode)
                .or(() -> priceRepo.findBestPrice(plan.getPlanId(), terms.getPtermId(), mode))
                .orElseThrow(() -> new IllegalStateException("가격 정보가 없습니다. plan=" + planCode + ", months=" + months));

        final BigDecimal amount = price.getPpriceAmount();
        final String currency = StringUtils.hasText(price.getPpriceCurr()) ? price.getPpriceCurr() : "KRW";

        PlanMember pm = planMemberRepo.findByMember_Mid(mid).orElse(null);
        LocalDateTime now = LocalDateTime.now();

        if (pm == null) {
            pm = PlanMember.builder()
                    .member(member)
                    .payment(payment)
                    .plan(plan)
                    .terms(terms)
                    .price(price)
                    .pmStatus(PmStatus.ACTIVE)
                    .pmBilMode(months == 1 ? PmBillingMode.MONTHLY : PmBillingMode.PREPAID_TERM)
                    .pmStart(now)
                    .pmTermStart(now)
                    .pmTermEnd(now.plusMonths(months))
                    .pmNextBil(now.plusMonths(months))
                    .pmCycle(months)
                    .pmCancelCheck(false)
                    .cancelAtPeriodEnd(false)
                    .build();
            pm = planMemberRepo.save(pm);
            log.info("[subscriptions/charge-and-confirm] PlanMember created mid={}, pmId={}", mid, pm.getPmId());

        } else {
            // 🔒 만약 기간말 해지 예약이고 주기가 끝났으면 재구독 필요
            if (Boolean.TRUE.equals(pm.isCancelAtPeriodEnd())
                    && pm.getPmTermEnd() != null
                    && !now.isBefore(pm.getPmTermEnd())) {
                throw new IllegalStateException("해지 예약된 구독입니다. 주기 종료 이후에는 재구독이 필요합니다.");
            }
            // 기존 멤버라도 이번 결제수단 업데이트
            pm.setPayment(payment);
            planMemberRepo.save(pm);
        }

        // 최근 PENDING 재사용(중복 생성 방지)
        LocalDateTime nowTrunc = now.truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime from = nowTrunc.minusMinutes(10);
        var recentOpt = invoiceRepo.findRecentPendingSameAmount(
                mid, PiStatus.PENDING, amount, currency, from, nowTrunc
        );

        PlanInvoiceEntity invoice;
        if (recentOpt.isPresent()) {
            invoice = recentOpt.get();
            log.info("[subscriptions/charge-and-confirm] reuse pending invoice mid={}, invoiceId={}", mid, invoice.getPiId());
        } else {
            invoice = PlanInvoiceEntity.builder()
                    .planMember(pm)
                    .piStart(now)
                    .piEnd(now.plusMonths(months))
                    .piAmount(amount)
                    .piCurr(currency)
                    .piStat(PiStatus.PENDING)
                    .build();
            invoiceRepo.save(invoice);
            log.info("[subscriptions/charge-and-confirm] PENDING_CREATED mid={}, invoiceId={}", mid, invoice.getPiId());
        }

        return chargeByBillingKeyAndConfirm(invoice.getPiId(), mid, months);
    }

    /* =========================
     *  기간말 해지 / 취소 / 최종화
     * ========================= */

    @Override
    @Transactional
    public void scheduleCancelAtPeriodEnd(Long pmId, String mid) {
        PlanMember pm = planMemberRepo.findById(pmId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "PM_NOT_FOUND"));

        // 소유자 검증
        if (pm.getMember() == null || pm.getMember().getMid() == null || !pm.getMember().getMid().equals(mid)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "FORBIDDEN");
        }

        // 이미 완료면 멱등 처리
        if (pm.getPmStatus() == PmStatus.CANCELED) return;

        // 현재 주기 정보가 없는 경우: 즉시 해지
        if (pm.getPmTermEnd() == null) {
            int n = planMemberRepo.finalizeCancel(pmId, LocalDateTime.now());
            if (n != 1) throw new IllegalStateException("FINALIZE_CANCEL_FAILED");
            return;
        }

        // 오늘이 주기 종료 전이면 예약, 지났으면 즉시 해지
        if (LocalDateTime.now().isBefore(pm.getPmTermEnd())) {
            int n = planMemberRepo.markCancelAtPeriodEnd(pmId, true, LocalDateTime.now(), PmStatus.CANCEL_SCHEDULED);
            if (n != 1) throw new IllegalStateException("MARK_CANCEL_AT_PERIOD_END_FAILED");
        } else {
            int n = planMemberRepo.finalizeCancel(pmId, LocalDateTime.now());
            if (n != 1) throw new IllegalStateException("FINALIZE_CANCEL_FAILED");
        }
    }

    @Override
    @Transactional
    public void revertCancelAtPeriodEnd(Long pmId, String mid) {
        PlanMember pm = planMemberRepo.findById(pmId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "PM_NOT_FOUND"));

        // 소유자 검증
        if (pm.getMember() == null || pm.getMember().getMid() == null || !pm.getMember().getMid().equals(mid)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "FORBIDDEN");
        }

        // 해지 예약 상태에서만, 아직 기간이 남아있을 때만 복구
        if (pm.getPmStatus() != PmStatus.CANCEL_SCHEDULED) return; // 멱등
        if (pm.getPmTermEnd() != null && !LocalDateTime.now().isBefore(pm.getPmTermEnd())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "PERIOD_ENDED");
        }

        int n = planMemberRepo.revertCancelAtPeriodEnd(pmId);
        if (n != 1) throw new IllegalStateException("REVERT_CANCEL_AT_PERIOD_END_FAILED");
    }

    @Override
    @Transactional
    public void finalizeCancelIfDue(Long pmId) {
        PlanMember pm = planMemberRepo.findById(pmId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "PM_NOT_FOUND"));

        if (!pm.isCancelAtPeriodEnd() || pm.getPmStatus() != PmStatus.CANCEL_SCHEDULED) return;
        if (pm.getPmTermEnd() == null) return;

        if (!LocalDateTime.now().isBefore(pm.getPmTermEnd())) {
            int n = planMemberRepo.finalizeCancel(pmId, LocalDateTime.now());
            if (n != 1) throw new IllegalStateException("FINALIZE_CANCEL_FAILED");
        }
    }

    /* =========================
     *  (기존) 다음 결제 예약 해지
     * ========================= */

    @Override
    @Transactional
    public CancelNextResult cancelNextRenewal(String mid, String reason) {
        if (!StringUtils.hasText(mid)) {
            throw new IllegalStateException("LOGIN_REQUIRED");
        }
        final LocalDateTime now = LocalDateTime.now();

        // 활성 구독
        PlanMember active = planMemberRepo.findActiveByMid(mid, now)
                .orElseThrow(() -> new IllegalStateException("ACTIVE_SUBSCRIPTION_NOT_FOUND"));

        // billingKey 확보
        String billingKey = (active.getPayment() != null ? active.getPayment().getPayKey() : null);
        if (!StringUtils.hasText(billingKey)) {
            billingKey = paymentRepo.findTopByMidOrderByPayIdDesc(mid)
                    .map(PlanPaymentEntity::getPayKey)
                    .orElse(null);
        }
        if (!StringUtils.hasText(billingKey)) {
            throw new IllegalStateException("BILLING_KEY_NOT_FOUND");
        }

        // 프로젝트에는 autoRenew 플래그가 없으므로, 예약 취소 의사만 표기
        boolean autoRenewDisabled = false;
        active.setPmCancelCheck(true);
        planMemberRepo.save(active);

        // 다가오는 PENDING 인보이스 취소
        List<PlanInvoiceEntity> upcoming = invoiceRepo.findUpcomingPendingByPmId(active.getPmId(), now);
        boolean upcomingCanceled = false;
        for (PlanInvoiceEntity inv : upcoming) {
            inv.setPiStat(PiStatus.CANCELED);
            invoiceRepo.save(inv);
            upcomingCanceled = true;
        }

        // 포트원 예약 취소
        var pg = portoneClient.cancelPaymentSchedules(billingKey, null);
        boolean pgScheduleCanceled = pg.revokedScheduleIds() != null && !pg.revokedScheduleIds().isEmpty();

        log.info("[CancelNext] mid={}, billingKey={}, upcomingCanceled={}, pgRevoked={}",
                mid, billingKey, upcomingCanceled, pg.revokedScheduleIds());

        String msg = (!upcomingCanceled && !pgScheduleCanceled) ? "NO_CHANGE" : "OK";

        return new CancelNextResult(autoRenewDisabled, upcomingCanceled, pgScheduleCanceled, msg);
    }

    /* =========================
     *  헬퍼들
     * ========================= */

    private void updatePaymentCardMetaIfPresent(PlanPaymentEntity payment, JsonNode root) {
        if (payment == null || root == null || root.isMissingNode()) return;

        String last4 = firstNonBlank(
                root.at("/method/card/number").asText(null),
                root.at("/card/number/last4").asText(null),
                root.at("/payment/card/number/last4").asText(null),
                root.at("/card/last4").asText(null),
                root.at("/payment/card/last4").asText(null)
        );
        if (last4 != null) {
            String digits = last4.replaceAll("\\D", "");
            if (digits.length() >= 4) last4 = digits.substring(digits.length() - 4);
            else last4 = null;
        }

        String bin = firstNonBlank(
                root.at("/method/card/bin").asText(null),
                root.at("/card/number/bin").asText(null),
                root.at("/payment/card/number/bin").asText(null),
                root.at("/card/bin").asText(null),
                root.at("/payment/card/bin").asText(null)
        );
        String brand = firstNonBlank(
                root.at("/method/card/name").asText(null),
                root.at("/method/card/publisher").asText(null),
                root.at("/method/card/issuer").asText(null),
                root.at("/method/card/brand").asText(null),
                root.at("/card/brand").asText(null),
                root.at("/payment/card/brand").asText(null),
                root.at("/method/card/scheme").asText(null)
        );
        String pg = firstNonBlank(
                root.at("/channel/pgProvider").asText(null),
                root.path("pgProvider").asText(null),
                root.at("/payment/pgProvider").asText(null),
                root.at("/provider/pg").asText(null)
        );

        boolean changed = false;
        if (StringUtils.hasText(bin)   && !bin.equals(payment.getPayBin()))     { payment.setPayBin(bin);       changed = true; }
        if (StringUtils.hasText(brand) && !brand.equals(payment.getPayBrand())) { payment.setPayBrand(brand);   changed = true; }
        if (StringUtils.hasText(last4) && !last4.equals(payment.getPayLast4())) { payment.setPayLast4(last4);   changed = true; }
        if (StringUtils.hasText(pg)    && !pg.equals(payment.getPayPg()))       { payment.setPayPg(pg);         changed = true; }

        if (!changed) {
            log.debug("[PaymentMeta] nothing to update for mid={}", payment.getMid());
            return;
        }

        paymentRepo.save(payment);
        log.info("[PaymentMeta] updated mid={}, bin={}, brand={}, last4={}, pg={}",
                payment.getMid(), payment.getPayBin(), payment.getPayBrand(), payment.getPayLast4(), payment.getPayPg());
    }

    private boolean isPaid(String s){ return isTerminal(s) && !"FAILED".equalsIgnoreCase(s) && !"CANCELLED".equalsIgnoreCase(s) && !"CANCELED".equalsIgnoreCase(s); }
    private boolean isFailed(String s){ String u = (s==null?"":s).trim().toUpperCase(); return u.equals("FAILED") || u.equals("CANCELLED") || u.equals("CANCELED"); }
    private boolean isTerminal(String s) {
        if (s == null) return false;
        String u = s.trim().toUpperCase();
        return u.equals("PAID") || u.equals("SUCCEEDED") || u.equals("SUCCESS")
                || u.equals("PARTIAL_PAID") || u.equals("FAILED") || u.equals("CANCELLED") || u.equals("CANCELED");
    }

    private String firstNonBlank(String... vals) {
        if (vals == null) return null;
        for (String v : vals) if (v != null && !v.isBlank()) return v;
        return null;
    }

    private JsonNode firstPaymentNode(JsonNode root) {
        if (root == null || root.isMissingNode()) return om.createObjectNode();
        if (root.isArray()) return root.size() > 0 ? root.get(0) : om.createObjectNode();
        if (root.has("items") && root.path("items").isArray()) {
            JsonNode arr = root.path("items");
            return arr.size() > 0 ? arr.get(0) : om.createObjectNode();
        }
        if (root.has("content") && root.path("content").isArray()) {
            JsonNode arr = root.path("content");
            return arr.size() > 0 ? arr.get(0) : om.createObjectNode();
        }
        return root;
    }

    private String pickStatus(JsonNode n) {
        String s = pick(n, "status");
        if (s == null) s = pick(n.path("payment"), "status");
        return s;
    }
    private String pick(JsonNode n, String field) {
        if (n == null || n.isMissingNode()) return null;
        String v = n.path(field).asText(null);
        return (v == null || v.isBlank()) ? null : v;
    }
    private JsonNode safeJson(String s) {
        try { return om.readTree(s == null ? "{}" : s); }
        catch (Exception e) { return om.createObjectNode(); }
    }
}
