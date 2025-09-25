package com.dodam.plan.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="PLANBENEFIT")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlanBenefitEntity {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long pbId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name="planId", nullable=false, foreignKey=@ForeignKey(name="fk_benefit_plan"))
  private PlansEntity plan;

  @Column(nullable=true,precision=11, scale=2) private BigDecimal pbPriceCap;
  @Lob private String pbNote;
}
