package com.dodam.delivery.entity;

import java.math.BigDecimal;

import com.dodam.member.entity.MemberEntity;
import com.dodam.product.entity.ProductEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "deliveryman")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliverymanEntity {

    @Id
    @Column(name = "delnum")
    private Long delnum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pronum", nullable = false)
    private ProductEntity product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mnum", nullable = false)
    private MemberEntity member;

    @Column(name = "dayoff")
    private Integer dayoff;

    @Column(name = "delcost", precision = 10, scale = 2)
    private BigDecimal delcost;

    @Column(name = "location")
    private String location;
}