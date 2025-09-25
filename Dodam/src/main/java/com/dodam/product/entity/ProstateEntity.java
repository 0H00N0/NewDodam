package com.dodam.product.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "prostate")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProstateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "prosnum")
    private Long prosnum;

    // 정의서: varchar NULL (S,A,B,C 등급)
    @Column(name = "prograde", length = 10)
    private String prograde;
}
