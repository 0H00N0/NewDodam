// src/main/java/com/dodam/plan/Entity/PlanPaymentEntity.java
package com.dodam.plan.Entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(
    name = "PLANPAYMENT",
    uniqueConstraints = {
        @UniqueConstraint(name = "UK_PLANPAYMENT_MID_KEY", columnNames = {"mid", "payKey"})
        // 만약 payKey 단독 유니크라면 위 줄 대신 @Column(unique=true) on payKey 로 사용하세요.
    }
)
@SequenceGenerator(
    name = "PLANPAYMENT_SEQ_GEN",
    sequenceName = "PLANPAYMENT_SEQ",
    allocationSize = 1
)
public class PlanPaymentEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "payId")
	private Long payId;

    @Column(name = "mid", nullable = false, length = 50)
    private String mid;

    @Column(name = "payKey", nullable = false, length = 200)
    private String payKey;           // = billingKey

    @Column(name = "payCustomer", length = 200)
    private String payCustomer;      // PortOne customerId

    @Column(name = "payBrand", length = 50)
    private String payBrand;         // 카드 브랜드

    @Column(name = "payBin", length = 8)
    private String payBin;           // BIN

    @Column(name = "payLast4", length = 4)
    private String payLast4;

    @Column(name = "payPg", length = 50)
    private String payPg;            // PG사 (tosspayments 등)

    @Column(name = "payCreatedAt", nullable = false)
    private LocalDateTime payCreatedAt;

    @Lob
    @Column(name = "payRaw")
    private String payRaw;           // 원문 JSON (안전 보관용)
}
