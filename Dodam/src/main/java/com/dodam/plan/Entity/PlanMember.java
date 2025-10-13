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
    // ✅ 상태 인덱스는 실제 컬럼명(pmStatus)에 맞춘다
    @Index(name="idx_pm_status",  columnList="pmStatus"),
    @Index(name="idx_pm_nextbil", columnList="pmNextBil"),
    @Index(name="idx_pm_plan",    columnList="planId,ptermId")
  }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlanMember {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
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
   *   상태/과금 모드
   * ========================= */

  // ⚠️ 구 컬럼(pmStat)과 혼동을 피하기 위해 엔티티에선 제거
  // @Enumerated(EnumType.STRING)
  // @Column(nullable = false, length = 20)
  // private PmStatus pmStat;

  @Enumerated(EnumType.STRING)
  @Column(name = "pmStatus", length = 30, nullable = false)
  private PmStatus pmStatus = PmStatus.ACTIVE;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PmBillingMode pmBilMode;  // MONTHLY / PREPAID_TERM

  /* =========================
   *   기간 / 갱신
   * ========================= */
  @Column(nullable=false)
  private LocalDateTime pmStart = LocalDateTime.now();

  private LocalDateTime pmTermStart;
  private LocalDateTime pmTermEnd;
  private LocalDateTime pmNextBil;
  private Integer pmCycle;

  /* =========================
   *   해지(기간말 예약) 관련
   * ========================= */

  // (기존 로직과의 호환 용도: 필요하면 유지, 아니면 제거해도 무방)
  @Column(nullable=false)
  private boolean pmCancelCheck = false;

  /** 해지 신청 시각 (DDL: pmCancelReqAt) */
  @Column(name = "pmCancelReqAt")
  private LocalDateTime cancelRequestedAt;

  /** 기간말 해지 예약 여부 (DDL: pmCancelAtEnd NUMBER(1) NOT NULL) */
  @Column(name = "pmCancelAtEnd", nullable = false)
  private boolean cancelAtPeriodEnd = false;

  /** 실제 해지 완료 시각 (DDL: pmCanceledAt) */
  @Column(name = "pmCanceledAt")
  private LocalDateTime canceledAt;
}
