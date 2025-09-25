package com.dodam.event.entity;

import java.time.LocalDateTime;

import com.dodam.member.entity.MemberEntity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Drawing")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Drawing {

    @Id
    @Column(name = "drawNum", nullable = false)
    private Long drawNum;   // PK: 추첨 참여 고유번호

    @ManyToOne
    @JoinColumn(name = "evNum", nullable = false)
    private EventNumber event;   // FK: 이벤트 번호

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mNum", nullable = false)
    private MemberEntity member;   // FK: 회원 참조

    @ManyToOne
    @JoinColumn(name = "lotNum", nullable = false)
    private LotteryTicket lotteryTicket;   // FK: 추첨권 번호

    @Column(name = "proNum")
    private Long proNum;   // FK: 상품 번호

    @Column(name = "drawState", nullable = false)
    private Integer drawState = 0;   // 0=미당첨, 1=당첨, 2=무효

    @Column(name = "drawDate")
    private LocalDateTime drawDate;   // 추첨 실행 시간
}

