package com.dodam.plan.dto;

import java.util.List;

import com.dodam.plan.Entity.PlanBenefitEntity;
import com.dodam.plan.Entity.PlanPriceEntity;
import com.dodam.plan.Entity.PlansEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.Comparator;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlanDetailDTO {
  private Long planId;
  private String planCode;
  private String displayName;
  private Long rentalPriceCapInt;    // 프런트가 쓰는 키 그대로
  private String note;
  private Boolean isActive;

  @Builder @Getter @AllArgsConstructor
  public static class PlanPriceItem {
    private Integer months;
    private Long amountInt;
    private Integer discountRate; // 없으면 0
  }
  private List<PlanPriceItem> planPrices;
  private List<String> benefits;

  public static PlanDetailDTO of(PlansEntity p, PlanBenefitEntity b, List<PlanPriceEntity> priceList) {
    return PlanDetailDTO.builder()
      .planId(p.getPlanId())
      .planCode(p.getPlanCode())
      .displayName(p.getPlanName() != null ? p.getPlanName().getPlanName() : p.getPlanCode())
      .rentalPriceCapInt(b != null && b.getPbPriceCap()!=null ? b.getPbPriceCap().longValue() : 0L)
      .note(b != null ? b.getPbNote() : null)
      .isActive(Boolean.TRUE.equals(p.getPlanActive()))
      .planPrices(priceList == null ? List.of() :
        priceList.stream()
          .filter(pp -> Boolean.TRUE.equals(pp.getPpriceActive()))
          .sorted(Comparator.comparing(PlanPriceEntity::getPpriceAmount)) // 혹은 months 기준
          .map(pp -> new PlanPriceItem(
              pp.getPterm() != null ? pp.getPterm().getPtermMonth() : null,
              pp.getPpriceAmount() != null ? pp.getPpriceAmount().longValue() : 0L,
              0 // 할인율 로직 있으면 계산
          ))
          .toList()
      )
      .benefits(b != null && b.getPbNote()!=null ? List.of(b.getPbNote()) : List.of()) // 혜택 여러 개면 적절히 매핑



.build();
  }
}
