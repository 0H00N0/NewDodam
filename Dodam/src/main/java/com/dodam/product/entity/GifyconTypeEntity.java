package com.dodam.product.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "gifycontype") // 정의서 표기 그대로 사용
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GifyconTypeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "giftcate")
    private Long giftcate;

    @Column(name = "giftcatename", length = 100)
    private String giftcatename;
}
