package com.dodam.plan.dto;

import java.util.List;
import java.util.Comparator;

import com.dodam.plan.Entity.PlanBenefitEntity;
import com.dodam.plan.Entity.PlanPriceEntity;
import com.dodam.plan.Entity.PlansEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanDetailDTO {

  private Long planId;
  private String planCode;
  private String displayName;
  private Long rentalPriceCapInt; // 프런트가 쓰는 키 그대로
  private String note;
  private Boolean isActive;

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class PlanPriceItem {
    private Long ppriceId;        // 프론트에서 priceId로 사용
    private Integer months;       // 기간(개월)
    private Long amountInt;       // 총액(원)
    private Integer discountRate; // 할인율(없으면 0)
  }

  private List<PlanPriceItem> planPrices;
  private List<String> benefits;

  public static PlanDetailDTO of(PlansEntity p, PlanBenefitEntity b, List<PlanPriceEntity> priceList) {
    return PlanDetailDTO.builder()
        .planId(p.getPlanId())
        .planCode(p.getPlanCode())
        .displayName(p.getPlanName() != null ? p.getPlanName().getPlanName() : p.getPlanCode())
        .rentalPriceCapInt(b != null && b.getPbPriceCap() != null ? b.getPbPriceCap().longValue() : 0L)
        .note(b != null ? b.getPbNote() : null)
        .isActive(Boolean.TRUE.equals(p.getPlanActive()))
        .planPrices(
            priceList == null ? List.of()
                : priceList.stream()
                    .filter(pp -> Boolean.TRUE.equals(pp.getPpriceActive()))
                    // 필요 시 months 기준으로 정렬하려면 comparator 교체
                    .sorted(Comparator.comparing(PlanPriceEntity::getPpriceAmount))
                    .map(pp -> PlanPriceItem.builder()
                        .ppriceId(pp.getPpriceId())
                        .months(pp.getPterm() != null ? pp.getPterm().getPtermMonth() : null)
                        .amountInt(pp.getPpriceAmount() != null ? pp.getPpriceAmount().longValue() : 0L)
                        .discountRate(0) // 할인율 로직 있으면 계산하여 반영
                        .build())
                    .toList())
        .benefits(b != null && b.getPbNote() != null ? List.of(b.getPbNote()) : List.of())
        .build();
  }
}
