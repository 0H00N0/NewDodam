package com.dodam.plan.service;

import com.dodam.plan.Entity.PlanInvoiceEntity;
import com.dodam.plan.enums.PlanEnums.PiStatus;
import com.dodam.plan.repository.PlanInvoiceRepository;
import com.dodam.plan.repository.PlanPaymentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

/**
 * PortOne/Toss v2 webhook event handler (컴파일 오류 없는 최소 구현).
 * - 인보이스: piUid/PAID 세팅
 * - 시도(Attempt): PlanBillingService.recordAttempt(...) 로 기록 (pattUrl 포함)
 * - 카드메타: billingKey(=payKey) 기준으로만 업데이트 (다른 카드 오염 방지)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanPaymentEventService {

    private final PlanInvoiceRepository invoiceRepo;
    private final PlanPaymentRepository paymentRepo;
    private final PlanBillingService billingSvc; // Attempt 기록/원문 저장 담당

    /* ===== paid ===== */
    @Transactional
    public void onPaid(JsonNode root) {
        JsonNode payload = root == null ? null : root.path("payload");

        String paymentId = pick(payload,
                "paymentId","id","uid","merchantUid","txId","transactionUid","transaction_uid","tx_id");
        if (!StringUtils.hasText(paymentId)) {
            log.warn("[paid] missing paymentId. payload={}", safe(payload));
            return;
        }

        // 카드 메타 + billingKey
        String brand  = pick(payload, "card.company","card.brand","card.issuer","methodDetail.brand");
        String bin    = pick(payload, "card.bin"); // 제공될 때만
        String last4  = firstNonBlank(
                pick(payload,"card.lastFourDigits","card.last4"),
                tail4(pick(payload,"card.number","card.cardNumber"))
        );
        String pgName = firstNonBlank(pick(payload,"pgProvider","gateway","pg","provider"));
        String payKey = firstNonBlank(pick(payload,"billingKey","billing_key","payKey"));

        // 영수증 URL
        String receiptUrl = firstNonBlank(
                pick(payload,"receiptUrl","card.receiptUrl","cashReceipt.url"));

        // 1) 인보이스 찾기 (piUid=paymentId → 실패 시 inv{piId}-ts 형태 파싱)
        Optional<PlanInvoiceEntity> optInv = invoiceRepo.findByPiUid(paymentId);
        if (optInv.isEmpty()) {
            optInv = parseInvoiceIdFrom(paymentId).flatMap(invoiceRepo::findById);
        }

        if (optInv.isPresent()) {
            PlanInvoiceEntity inv = optInv.get();

            // 이미 PAID면 누락 보조필드만 보정
            if (PiStatus.PAID.equals(inv.getPiStat())) {
                if (!StringUtils.hasText(inv.getPiUid())) {
                    invoiceRepo.markPaidAndSetUidIfEmpty(inv.getPiId(), paymentId, LocalDateTime.now());
                }
                // Attempt(성공) 기록 + 영수증 URL 저장
                billingSvc.recordAttempt(inv.getPiId(), true, null, paymentId, receiptUrl, safe(root));
            } else {
                // PAID 전환 + piUid 세팅
                invoiceRepo.markPaidAndSetUidIfEmpty(inv.getPiId(), paymentId, LocalDateTime.now());
                // Attempt(성공) 기록 + 영수증 URL 저장
                billingSvc.recordAttempt(inv.getPiId(), true, null, paymentId, receiptUrl, safe(root));
            }
        } else {
            // 인보이스 미매칭: 시도 로그만 남김 (후속 조정 가능)
            log.warn("[paid] invoice not found for paymentId={}", paymentId);
            // invoiceId 필요 파라미터라면 0L 같은 더미를 쓰지 말고, 여기서는 기록 생략/로깅만 유지
            // billingSvc.recordAttempt(...) 가 invoiceId 필수라면 호출하지 않습니다.
        }

        // 2) 카드 메타: billingKey 있으면 해당 카드에만 업데이트
        if (StringUtils.hasText(payKey)) {
            paymentRepo.updateCardMetaByKey(payKey, nz(bin), nz(brand), nz(last4), nz(pgName));
        }
    }

    /* ===== 취소/부분결제 등은 필요 시 최소화해 처리 ===== */
    @Transactional
    public void onPartiallyPaid(JsonNode root) {
        log.info("[partially_paid] payload={}", safe(root));
        // 필요 시 onPaid와 유사하게 처리
    }

    @Transactional
    public void onCancelled(JsonNode root) {
        log.info("[cancelled] payload={}", safe(root));
        // 취소 반영 필요 시: paymentId → invoice 찾은 뒤 piStat 업데이트
        // 단, 현재 프로젝트 레포지토리에 findByPiUidForUpdate 가 없어 안전하게 생략
    }

    /* ===== helpers ===== */

    private static String pick(JsonNode root, String... paths) {
        if (root == null) return null;
        for (String p : paths) {
            String v = path(root, p);
            if (StringUtils.hasText(v)) return v;
        }
        return null;
    }

    private static String path(JsonNode root, String dotPath) {
        if (root == null) return null;
        JsonNode cur = root;
        for (String seg : dotPath.split("\\.")) {
            cur = cur.path(seg);
            if (cur.isMissingNode() || cur.isNull()) return null;
        }
        return cur.isValueNode() ? cur.asText(null) : null;
    }

    private static String tail4(String masked) {
        if (!StringUtils.hasText(masked)) return null;
        String s = masked.replaceAll("[^0-9]", "");
        if (s.length() < 4) return null;
        return s.substring(s.length() - 4);
    }

    private static String firstNonBlank(String... v){
        if (v == null) return null;
        for (String s : v) if (StringUtils.hasText(s)) return s;
        return null;
    }

    private static Optional<Long> parseInvoiceIdFrom(String anyId) {
        try {
            String onlyNum = anyId == null ? "" : anyId
                    .replaceFirst("^inv","")
                    .split("-")[0]
                    .replaceAll("[^0-9]","");
            if (onlyNum.isEmpty()) return Optional.empty();
            return Optional.of(Long.parseLong(onlyNum));
        } catch (Exception ignore) {
            return Optional.empty();
        }
    }

    private static String nz(String s){ return s == null ? null : s.trim(); }
    private static String safe(JsonNode n){ try { return n == null ? null : n.toString(); } catch (Exception e){ return null; } }
    @SuppressWarnings("unused")
    private static String up(String s){ return s==null? null : s.trim().toUpperCase(Locale.ROOT); }
}
