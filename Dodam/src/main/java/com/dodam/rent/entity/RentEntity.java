	package com.dodam.rent.entity;

import com.dodam.member.entity.MemberEntity;
import com.dodam.product.entity.ProductEntity;
import com.dodam.reservation.entity.Reservation;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Entity
@Table(name = "rent")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RentEntity { //대여 테이블

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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resernum", nullable = true)
    private Reservation reservation;

    @Column(name = "rendate", nullable = true)
    private LocalDateTime renDate;

    @Column(name = "retdate", nullable = true)
    private LocalDateTime retDate;

    @Column(name = "renrider")
    private String renRider;

    @Column(name = "renapproval", nullable = false)
    private Integer renApproval;

    @Column(name = "renship", nullable = true)
    private String renShip;

    @Column(name = "overdue", nullable = true)
    private Integer overDue; //연체

    @Column(name = "extend")
    private Integer extendInfo;

    @Column(name = "renloss")
    private Integer renLoss;

    @Column(name = "restate", nullable = false)
    private Integer reState;

    @Column(name = "trackingnumber")
    private String trackingNumber; // 운송장 번호
    // ▲▲▲▲▲ 소문자/camelCase 필드 추가 ▲▲▲▲▲

    // 데이터 저장 전 기본값 설정
    @PrePersist
    public void setDefaultValues() {
        if (renApproval == null) renApproval = 0;
        if (overDue == null) overDue = 0;
        if (extendInfo == null) extendInfo = 0;
        if (renLoss == null) renLoss = 0;
        if (reState == null) reState = 0;
    }
}