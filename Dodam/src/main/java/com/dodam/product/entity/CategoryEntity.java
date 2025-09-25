package com.dodam.product.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "category")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CategoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "catenum")
    private Long catenum;

    // 정의서: NULL 허용
    @Column(name = "catename", length = 100)
    private String catename;
}
