package com.dodam.cart.Entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "cart")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class CartEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cartnum; // PK

    @Column(nullable = false)
    private Long mnum; // FK: 회원번호

    @Column(nullable = false)
    private Long pronum; // FK: 상품번호

    @Column(nullable = false)
    private Long catenum; // FK: 카테고리번호
}