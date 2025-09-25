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
@Table(name="PLANNAME", uniqueConstraints = {@UniqueConstraint(name = "uk_PlanName_name", columnNames = "planName")})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlanNameEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long planNameId;
	
	@Column(nullable = false, length = 100, unique = true)
	private String planName;
}
