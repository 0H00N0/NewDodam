package com.dodam.board.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "board") // 게시판 테이블
public class BoardPostEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bnum", nullable = false)
    private Long bnum; // 글번호 (PK)

    @Column(name = "mnum", nullable = false)
    private Long mnum; // 회원번호 (FK: Member 테이블 참조)

    @Column(name = "mtnum", nullable = false)
    private Long mtnum; // 작성자 유형 번호 (관리자, 구매자, 배송기사 등)

    // --- 관계 매핑 ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bcanum", nullable = false)
    private BoardCategoryEntity boardCategory; // 카테고리 번호 (FK: BoardCategory 테이블 참조)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bsnum", nullable = false)
    private BoardStateEntity boardState; // 게시판 상태 번호 (FK: BoardState 테이블 참조)

    @Column(name = "bsub", length = 255)
    private String bsub; // 제목

    @Column(name = "bcontent", length = 4000) // TEXT 또는 충분한 varchar 길이
    private String bcontent; // 내용

    @Column(name = "bdate")
    private LocalDateTime bdate; // 작성일

    @Column(name = "bedate")
    private LocalDateTime bedate; // 수정일

    @Column(name = "mid", nullable = false, length = 255)
    private String mid; // 회원 ID (로그인 ID)

    @Column(name = "mnic", nullable = false, length = 255)
    private String mnic; // 작성자 닉네임
}