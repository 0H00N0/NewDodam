package com.dodam.plan.Entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name="PLANS")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlansEntity {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long planId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name="planNameId", nullable=false, foreignKey=@ForeignKey(name="fk_plan_planName"))
  private PlanNameEntity planName;

  @Column(nullable=false, length=30, unique=true)
  private String planCode;

  @Column(nullable=false)
  private Boolean planActive;

  @CreationTimestamp
  @Column(nullable=false)
  private LocalDateTime planCreate;
}
