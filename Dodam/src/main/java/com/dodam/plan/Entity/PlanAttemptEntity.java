// src/main/java/com/dodam/plan/Entity/PlanAttemptEntity.java
package com.dodam.plan.Entity;

import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

import com.dodam.plan.enums.PlanEnums.PattResult;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "PlanAttempt")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class PlanAttemptEntity {

@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
private Long pattId;

@ManyToOne(optional = false, fetch = FetchType.LAZY)
@JoinColumn(name = "piId", nullable = false)
private PlanInvoiceEntity invoice;

@Enumerated(EnumType.STRING)
@Column(name = "pattResult", nullable = false, length = 16)
private PattResult pattResult; // SUCCESS / FAIL

// ✅ 아래 4개는 널 허용 권장(선택)
@Column(name = "pattFail", nullable = true, length = 4000)
private String pattFail;

@Column(name = "pattUid", nullable = true, length = 128)
private String pattUid;

@Column(name = "pattUrl", nullable = true, length = 1024)
private String pattUrl;

@Lob
@Column(name = "pattResponse", nullable = true)
private String pattResponse;

@CreationTimestamp
@Column(name = "pattAt", updatable = false)  // nullable은 굳이 강제하지 않음(마이그레이션 안전)
private LocalDateTime pattAt;
}

