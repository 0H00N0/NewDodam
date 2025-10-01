package com.dodam.buy.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "buy")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class BuyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long buynum; // PK

    @Column(nullable = false)
    private Long mnum; // FK: 회원번호

    @Column(nullable = false)
    private Long pronum; // FK: 상품번호

    @Column(nullable = false)
    private Long catenum; // FK: 카테고리번호

    @Column(nullable = false)
    private Long prosnum; // FK: 상태번호

}