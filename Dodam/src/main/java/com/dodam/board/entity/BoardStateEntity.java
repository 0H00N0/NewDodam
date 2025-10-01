package com.dodam.board.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "boardstate") // 게시판 상태 테이블
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardStateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bsnum", nullable = false)
    private Long bsnum; // 상태 번호 (PK)

    @Column(name = "bsname", length = 255)
    private String bsname; // 상태 내용 (1-활성화, 2-비활성, 3-폐쇄)
}