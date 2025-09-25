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

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PmStatus pmStat;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PmBillingMode pmBilMode;  // 정확히 이 이름

  @Column(nullable=false) private LocalDateTime pmStart = LocalDateTime.now();
  private LocalDateTime pmTermStart;
  private LocalDateTime pmTermEnd;
  private LocalDateTime pmNextBil;
  private Integer pmCycle;

  @Column(nullable=false)
  private boolean pmCancelCheck = false;
}
