package com.dodam.event.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lotteryTicketType")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LotteryTicketType {

    @Id
    @Column(name = "lotTypeNum", nullable = false)
    private Long lotTypeNum;   // PK: 추첨권 종류 고유번호

    @Column(name = "lotTypeName")
    private String lotTypeName;   // 추첨권 이름 (일반, 프리미엄)

}
