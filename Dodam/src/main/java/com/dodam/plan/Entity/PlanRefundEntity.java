package com.dodam.plan.Entity;

import com.dodam.plan.enums.*;
import com.dodam.plan.enums.PlanEnums.PrefMethod;
import com.dodam.plan.enums.PlanEnums.PrefStatus;
import com.dodam.plan.enums.PlanEnums.PrefType;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
  name="PLANREFUND",
  indexes = {
    @Index(name="idx_planrefund_piid", columnList="piId"),
    @Index(name="idx_planrefund_stat", columnList="prefStat")
  }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlanRefundEntity {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long prefId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name="piId", nullable=false, foreignKey=@ForeignKey(name="fk_pref_pi"))
  private PlanInvoiceEntity invoice;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name="pattId", foreignKey=@ForeignKey(name="fk_pref_patt"))
  private PlanAttemptEntity attempt;

  @Enumerated(EnumType.STRING) @Column(nullable=false, length=20)
  private PrefType prefType;

  @Column(length=200) private String prefReason;
  @Column(nullable=false, precision=12, scale=2) private BigDecimal prefAmount;
  @Column(nullable=false, length=3) private String prefCurr = "KRW";

  @Enumerated(EnumType.STRING) @Column(nullable=false, length=20)
  private PrefStatus prefStat;

  @Enumerated(EnumType.STRING) @Column(nullable=false, length=20)
  private PrefMethod prefMethod;

  @Column(nullable=false) private LocalDateTime prefRequest = LocalDateTime.now();
  private LocalDateTime prefProcess;

  @Column(length=200) private String prefImpUid;
  @Column(length=200) private String prefCancelUid;
  @Column(length=200) private String prefCancelId;
  @Column(length=500) private String prefReceiptUrl;

  @Lob private String prefResponse; // 포트원 응답 원문
}
