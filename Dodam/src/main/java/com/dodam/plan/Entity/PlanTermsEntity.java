package com.dodam.plan.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "PLANTERMS",
		uniqueConstraints = @UniqueConstraint(name = "uk_planTerms_ptermMonth", columnNames = "ptermMonth"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlanTermsEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long ptermId;
	
	@Column(nullable = false)
	private Integer ptermMonth;
}
