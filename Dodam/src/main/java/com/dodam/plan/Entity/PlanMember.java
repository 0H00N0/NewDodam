package com.dodam.plan.Entity;

import com.dodam.member.entity.MemberEntity;
import com.dodam.plan.enums.PlanEnums.PmBillingMode;
import com.dodam.plan.enums.PlanEnums.PmStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
  name = "PLANMEMBER",
  indexes = {
    @Index(name="idx_pm_member",  columnList="mnum"),
    @Index(name="idx_pm_status",  columnList="pmStat"),
    @Index(name="idx_pm_nextBil", columnList="pmNextBil"),
    @Index(name="idx_pm_plan",    columnList="planId,ptermId")
  }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlanMember {

  @Id 
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long pmId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name="ppriceId", nullable=false, foreignKey=@ForeignKey(name="fk_pm_price"))
  private PlanPriceEntity price;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name="planId", nullable=false, foreignKey=@ForeignKey(name="fk_pm_plans"))
  private PlansEntity plan;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name="ptermId", nullable=false, foreignKey=@ForeignKey(name="fk_pm_terms"))
  private PlanTermsEntity terms;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name="payId", nullable=false, foreignKey=@ForeignKey(name="fk_pm_payment"))
  private PlanPaymentEntity payment;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name="mnum", nullable=false, foreignKey=@ForeignKey(name="fk_pm_member"))
  private MemberEntity member;

  /* =========================
   *   상태 / 과금 모드
   * ========================= */

  @Enumerated(EnumType.STRING)
  @Column(name = "pmStat", length = 30, nullable = false)
  @Builder.Default
  private PmStatus pmStatus = PmStatus.ACTIVE;

  @Enumerated(EnumType.STRING)
  @Column(name = "pmBilMode", nullable = false, length = 20)
  private PmBillingMode pmBilMode;

  /* =========================
   *   기간 / 갱신 관련
   * ========================= */
  @Column(name = "pmStart", nullable=false)
  @Builder.Default
  private LocalDateTime pmStart = LocalDateTime.now();

  @Column(name = "pmTermStart")
  private LocalDateTime pmTermStart;

  @Column(name = "pmTermEnd")
  private LocalDateTime pmTermEnd;

  @Column(name = "pmNextBil")
  private LocalDateTime pmNextBil;

  @Column(name = "pmCycle")
  private Integer pmCycle;

  /* =========================
   *   해지(기간말 예약) 관련
   * ========================= */
  @Column(name="pmCancelCheck", nullable=false)
  @Builder.Default
  private boolean pmCancelCheck = false;

  /** 해지 신청 시각 */
  @Column(name = "pmCancelReqAt")
  private LocalDateTime cancelRequestedAt;

  /** 기간말 해지 예약 여부 */
  @Column(name = "pmCancelAtEnd", nullable = false)
  @Builder.Default
  private boolean cancelAtPeriodEnd = false;

  /** 실제 해지 완료 시각 */
  @Column(name = "pmCanceledAt")
  private LocalDateTime canceledAt;
  
  /** 다음 결제 주기에 적용할 예정 가격/플랜/기간 (optional) */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name="nextPpriceId", foreignKey=@ForeignKey(name="fk_pm_next_price"))
  private PlanPriceEntity nextPrice;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name="nextPlanId", foreignKey=@ForeignKey(name="fk_pm_next_plans"))
  private PlansEntity nextPlan;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name="nextPtermId", foreignKey=@ForeignKey(name="fk_pm_next_terms"))
  private PlanTermsEntity nextTerms;
  
  public void clearPendingChange() {
	    this.nextPlan = null;
	    this.nextTerms = null;
	    this.nextPrice = null;
	}
}
