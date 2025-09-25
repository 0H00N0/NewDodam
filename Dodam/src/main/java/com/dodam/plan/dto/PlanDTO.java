package com.dodam.plan.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.dodam.plan.Entity.PlanBenefitEntity;
import com.dodam.plan.Entity.PlanPriceEntity;
import com.dodam.plan.Entity.PlansEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanDTO {

    // ── 공통(목록/상세)
    private Long   planId;
    private String planCode;              // BASIC / Standard / ...
    private String displayName;           // 예: 베이직 (BASIC)
    private Long   priceInt;              // 대표 노출 가격(정수, 1개월 or 최저가 등 규칙에 따름)
    private Long   rentalPriceCapInt;     // 월 대여 상한(정수) — 0이면 "제한 없음"
    private String currency;              // 통화 (기본 KRW)
    private String note;                  // 혜택 설명 (pbNote의 본문)

    // ── 상세 전용
    private List<PlanPriceDTO> planPrices; // 기간별 금액 목록
    private List<String>       benefits;   // 포함 혜택 리스트(파싱 결과)

    // ─────────────────────────────────────────────────────────────────
    // 기존 목록용 of — (변경 최소화) 목록 페이지는 이 팩토리 그대로 사용
    // ─────────────────────────────────────────────────────────────────
    public static PlanDTO of(PlansEntity p,
                             PlanBenefitEntity b,
                             List<PlanPriceEntity> prices) {

        String disp = buildDisplayName(p);

        // 활성 가격들 중 "최저가"를 대표가로 선택 (필요 시 규칙 변경 가능)
        PlanPriceEntity chosen = null;
        if (prices != null && !prices.isEmpty()) {
            chosen = prices.stream()
                    .filter(pp -> Boolean.TRUE.equals(pp.getPpriceActive()))
                    .min(Comparator.comparing(PlanPriceEntity::getPpriceAmount))
                    .orElse(null);
        }

        BigDecimal amount = (chosen != null ? chosen.getPpriceAmount() : null);
        String curr = (chosen != null ? chosen.getPpriceCurr() : "KRW");

        BigDecimal cap = (b != null ? b.getPbPriceCap() : null);
        String note = (b != null ? b.getPbNote() : null);

        return PlanDTO.builder()
                .planId(p.getPlanId())
                .planCode(p.getPlanCode())
                .displayName(disp)
                .priceInt(toLong(amount))
                .rentalPriceCapInt(toLong(cap))
                .currency(curr == null ? "KRW" : curr)
                .note(note)
                // 상세 전용 필드는 목록에선 비움
                .planPrices(null)
                .benefits(null)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────
    // 상세용 of — 파싱된 note/benefits와 기간별 가격 리스트 포함
    // PlanService에서 parseBenefitNote() 호출 후 이 팩토리로 조립
    // ─────────────────────────────────────────────────────────────────
    public static PlanDTO of(PlansEntity p,
                             PlanBenefitEntity b,
                             List<PlanPriceEntity> prices,
                             String parsedNote,
                             List<String> parsedBenefits) {

        String disp = buildDisplayName(p);

        // 대표가 산정 로직(최저가) — 목록과 동일하게 계산
        PlanPriceEntity chosen = null;
        if (prices != null && !prices.isEmpty()) {
            chosen = prices.stream()
                    .filter(pp -> Boolean.TRUE.equals(pp.getPpriceActive()))
                    .min(Comparator.comparing(PlanPriceEntity::getPpriceAmount))
                    .orElse(null);
        }

        BigDecimal amount = (chosen != null ? chosen.getPpriceAmount() : null);
        String curr = (chosen != null ? chosen.getPpriceCurr() : "KRW");

        BigDecimal cap = (b != null ? b.getPbPriceCap() : null);

        // 기간별 가격 목록 매핑
        List<PlanPriceDTO> priceList =
            prices == null ? List.of() :
            prices.stream()
                .filter(pp -> Boolean.TRUE.equals(pp.getPpriceActive()))
                .map(PlanDTO::mapPrice)         // 아래 helper 사용
                .filter(Objects::nonNull)
                // months 기준 정렬(null은 맨 뒤)
                .sorted((x, y) -> {
                    Integer mx = x.getMonths();
                    Integer my = y.getMonths();
                    if (mx == null && my == null) return 0;
                    if (mx == null) return 1;
                    if (my == null) return -1;
                    return Integer.compare(mx, my);
                })
                .toList();

        return PlanDTO.builder()
                .planId(p.getPlanId())
                .planCode(p.getPlanCode())
                .displayName(disp)
                .priceInt(toLong(amount))
                .rentalPriceCapInt(toLong(cap))
                .currency(curr == null ? "KRW" : curr)
                .note(parsedNote)                          // ← 파싱된 설명
                .planPrices(priceList)                     // ← 상세 리스트
                .benefits(parsedBenefits == null ? List.of() : parsedBenefits) // ← 파싱된 혜택
                .build();
    }

    // ─────────────────────────────────────────────────────────────────
    // Helper들
    // ─────────────────────────────────────────────────────────────────
    private static String buildDisplayName(PlansEntity p) {
        String code = p.getPlanCode() != null ? p.getPlanCode() : "";
        String name = (p.getPlanName() != null && p.getPlanName().getPlanName() != null)
                ? p.getPlanName().getPlanName()
                : code;
        return name + " (" + code + ")";
    }

    private static Long toLong(BigDecimal v) {
        if (v == null) return 0L; // 0이면 프론트에서 "제한 없음" 등으로 처리 용이
        return v.setScale(0, RoundingMode.DOWN).longValue();
    }

    private static PlanPriceDTO mapPrice(PlanPriceEntity pp) {
        if (pp == null) return null;

        // months: PTERM에서 가져오되 null-safe
        Integer months = null;
        try {
            months = (pp.getPterm() != null) ? pp.getPterm().getPtermMonth() : null;
        } catch (Exception ignore) {}

        Long amountInt = toLong(pp.getPpriceAmount());
        Integer discountRate = 0; // 할인 계산 로직이 생기면 반영

        return new PlanPriceDTO(months, amountInt, discountRate);
    }

    // ── 상세 가격 DTO
    @Getter
    @AllArgsConstructor
    public static class PlanPriceDTO {
        private Integer months;     // 개월 수(1/3/6/12...)
        private Long    amountInt;  // 총액(정수)
        private Integer discountRate;
    }
}
