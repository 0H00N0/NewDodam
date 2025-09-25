package com.dodam.event.entity;

import java.time.LocalDateTime;

import com.dodam.member.entity.MemberEntity;
import com.dodam.member.entity.MemtypeEntity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lotteryTicket")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LotteryTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "lottery_ticket_seq")
    @SequenceGenerator(name = "lottery_ticket_seq", sequenceName = "LOTTERY_TICKET_SEQ", allocationSize = 1)
    @Column(name = "lotNum", nullable = false)
    private Long lotNum;   // 추첨권 번호 (PK)

    @Column(name = "lotCount", nullable = false)
    private Integer lotCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mnum", nullable = false)
    private MemberEntity member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mtnum", nullable = false)
    private MemtypeEntity memtype;

    // ✅ FK 매핑 (추첨권 종류)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lotTypeNum", nullable = false)
    private LotteryTicketType lotteryTicketType;

    @Column(name = "issuedAt", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "usedAt")
    private LocalDateTime usedAt;

    @Column(name = "status", nullable = false)
    private Integer status;  // 0=미사용, 1=사용, 2=만료
}


