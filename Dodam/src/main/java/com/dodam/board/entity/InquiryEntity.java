package com.dodam.board.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "inquiries",
    indexes = {
        @Index(name = "idx_inquiries_board_code", columnList = "boardcode"),
        @Index(name = "idx_inquiries_status", columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InquiryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 문의 번호 (PK)

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "boardcode", nullable = false)
    private BoardEntity board; // 게시판 코드 (FK)

    @Column(nullable = false, length = 200)
    private String title; // 제목

    @Lob
    @Column(nullable = false)
    private String content; // 내용

    @Column(name = "contact_email", length = 120)
    private String contactEmail; // 연락 이메일

    @Column(name = "issecret", nullable = false)
    private boolean secret = false; // 비밀글 여부

    @Column(name = "secretpassword", length = 120)
    private String secretPassword; // 비밀글 비밀번호

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InquiryStatus status = InquiryStatus.OPEN; // 상태 (OPEN, ANSWERED, CLOSED)

    @Lob
    @Column(name = "answer_content")
    private String answerContent; // 답변 내용

    @Column(name = "answered_at")
    private LocalDateTime answeredAt; // 답변 시간

    @Column(name = "createdat", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now(); // 등록일

    @Column(name = "updatedat")
    private LocalDateTime updatedAt; // 수정일

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum InquiryStatus {
        OPEN, ANSWERED, CLOSED
    }
}