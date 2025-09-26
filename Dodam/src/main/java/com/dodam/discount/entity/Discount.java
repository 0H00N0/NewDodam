package com.dodam.discount.entity;

import com.dodam.plan.Entity.PlanTermsEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "discount")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Discount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "disnum")
    private Long disNum;

    @Column(name = "dislevel", nullable = false)
    private Integer disLevel; // 1=대여할인, 2=구독할인

    @Column(name = "disvalue", nullable = false)
    private Integer disValue; // 할인율 %

    // ✅ planTerms와 연관관계 (기간 옵션 매핑)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ptermId", nullable = false)
    private PlanTermsEntity ptermId;
}
