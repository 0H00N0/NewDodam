package com.dodam.plan.Entity;

import com.dodam.plan.enums.PlanEnums.PiStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "PLANINVOICE", uniqueConstraints = @UniqueConstraint(name = "uk_pi_uid", columnNames = "piUid"), indexes = {
		@Index(name = "idx_planinvoice_pmid", columnList = "pmId"),
		@Index(name = "idx_planinvoice_stat", columnList = "piStat") })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanInvoiceEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long piId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "pmId", nullable = false, foreignKey = @ForeignKey(name = "fk_pi_pm"))
	private PlanMember planMember;

	public PlanMember getPlanMember() {
		return planMember;
	}

	@Column(nullable = false)
	private LocalDateTime piStart;
	@Column(nullable = false)
	private LocalDateTime piEnd;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal piAmount;
	@Column(nullable = false, length = 3)
	private String piCurr = "KRW";

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PiStatus piStat;

	@Column(length = 200)
	private String piUid; // v2 paymentId
	private LocalDateTime piPaid;
}
