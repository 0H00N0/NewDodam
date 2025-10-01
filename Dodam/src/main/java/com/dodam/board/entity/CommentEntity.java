package com.dodam.board.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "comment") // 게시판 댓글 테이블
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "comment_seq")
    @SequenceGenerator(
            name = "comment_seq",
            sequenceName = "COMMENT_SEQ", // DB 시퀀스명
            allocationSize = 1
    )
    @Column(name = "conum", nullable = false)
    private Long conum; // 댓글번호 (PK)

    @Column(name = "mnum", nullable = false)
    private Long mnum; // 회원번호 (FK: Member 참조)

    // Comment(N) : Board(1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bnum", nullable = false)
    private BoardEntity board; // 댓글이 달린 게시글

    @Column(name = "ccontent", length = 1000)
    private String ccontent; // 댓글 내용

    @Column(name = "mnic", nullable = false, length = 255)
    private String mnic; // 작성자 닉네임 (FK: Member 참조)

    @Column(name = "mid", nullable = false, length = 255)
    private String mid; // 회원 ID (FK: Member 참조)
}