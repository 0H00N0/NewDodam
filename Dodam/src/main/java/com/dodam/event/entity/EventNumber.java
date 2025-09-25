package com.dodam.event.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "EventNumber")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventNumber {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "event_number_seq")
    @SequenceGenerator(name = "event_number_seq", sequenceName = "EVENT_NUMBER_SEQ", allocationSize = 1)
    @Column(name = "evNum", nullable = false)
    private Long evNum; // PK: 이벤트 번호

    @Column(name = "evName", nullable = false)
    private String evName; // 이벤트 이름

    @Column(name = "evContent", nullable = false)
    private String evContent;   // 이벤트 설명

    @Column(name = "status", nullable = false)
    private Integer status = 0;   // 0=예정, 1=진행중, 2=종료

    @Column(name = "startTime")
    private LocalDateTime startTime;

    @Column(name = "endTime")
    private LocalDateTime endTime;

    @Column(name = "createdAt", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updatedAt", nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(nullable = false)
    private String eventType; // "FIRST" = 선착순, "DRAWING" = 추첨
    
    @Column(name = "capacity")
    private Integer capacity; // 선착순 이벤트 최대 인원
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lotTypeNum")
    private LotteryTicketType lotteryTicketType; // 이 이벤트에서 사용할 추첨권 타입


}

