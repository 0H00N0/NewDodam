package com.dodam.product.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reviewstate")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReviewStateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "revstatenum")
    private Long revstatenum;

    @Column(name = "revstate", length = 50) // 모두공개, 관리자만
    private String revstate;
}