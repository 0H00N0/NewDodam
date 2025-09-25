// src/main/java/com/dodam/plan/Entity/PlanPriceEntity.java
package com.dodam.plan.Entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name="PLANPRICE") // (필요하면 schema="DODAM" 추가)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlanPriceEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column
  private Long ppriceId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "PLANID",                  // ✅ 실제 FK 컬럼명으로 명시
      nullable = false,
      foreignKey = @ForeignKey(name = "FK_PRICE_PLAN")
  )
  private PlansEntity plan;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "PTERMID",                 // ✅ 실제 FK 컬럼명으로 명시
      nullable = false,
      foreignKey = @ForeignKey(name = "FK_PRICE_TERM")
  )
  private PlanTermsEntity pterm;

  @Column(nullable=false, length=20)
  private String ppriceBilMode;

  @Column(nullable=false, precision=12, scale=2)
  private BigDecimal ppriceAmount;

  @Column(nullable=false, length=3)
  private String ppriceCurr;

  @Column(nullable=false)
  private Boolean ppriceActive;

  @CreationTimestamp
  @Column(nullable=false)
  private LocalDateTime ppriceCreate;
}
