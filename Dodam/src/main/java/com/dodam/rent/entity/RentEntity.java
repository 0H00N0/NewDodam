package com.dodam.rent.entity;

import com.dodam.member.entity.MemberEntity;
import com.dodam.product.entity.ProductEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "rent")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RentEntity { // 대여 테이블

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rennum")
    private Long renNum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pronum", nullable = false)
    private ProductEntity product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mnum", nullable = false)
    private MemberEntity member;

    @Column(name = "rendate", nullable = true)
    private LocalDateTime renDate;   // 대여일

    @Column(name = "retdate", nullable = true)
    private LocalDateTime retDate;   // 반납일

    // @Column(name = "renrider", nullable = true)
    // private String renRider;   // 담당기사 (사용 안 함)

    // @Column(name = "renapproval", nullable = false)
    // private Integer renApproval;   // 승인 여부 (사용 안 함)

    // 배송 상태 ENUM
    public enum ShipStatus {
        SHIPPING,   // 배송중
        DELIVERED   // 배송완료
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "renship", nullable = true)
    private ShipStatus renShip;  // 배송 상태

    // @Column(name = "overdue", nullable = true)
    // private Integer overDue; // 연체 (사용 안 함)

    // @Column(name = "extend", nullable = true)
    // private Integer extendInfo; // 연장 횟수 (사용 안 함)

    // @Column(name = "renloss", nullable = true)
    // private Integer renLoss; // 분실/손실 여부 (사용 안 함)

    // @Column(name = "restate", nullable = true)
    // private Integer reState; // 회수 상태 (사용 안 함)

    @Column(name = "trackingnumber", nullable = true)
    private String trackingNumber; // 운송장 번호

    // ▲▲▲▲▲ 기본값 세팅 ▲▲▲▲▲
    @PrePersist
    public void setDefaultValues() {
        if (renShip == null) renShip = ShipStatus.SHIPPING; // 기본 배송 상태
    }
}
