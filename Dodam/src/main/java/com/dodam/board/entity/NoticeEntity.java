package com.dodam.board.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "notices",
    indexes = {
        @Index(name = "idx_notices_board_code", columnList = "boardcode"),
        @Index(name = "idx_notices_pinned", columnList = "ispinned")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 공지사항 번호 (PK)

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "boardcode", nullable = false)
    private BoardEntity board; // 게시판 코드 (FK)

    @Column(nullable = false, length = 200)
    private String title; // 제목

    @Lob
    @Column(nullable = false)
    private String content; // 내용

    @Column(name = "ispinned", nullable = false)
    private boolean pinned = false; // 상단 고정 여부

    @Column(nullable = false)
    private int views = 0; // 조회수

    @Column(nullable = false, length = 50)
    private String author; // 작성자

    @Column(name = "isactive", nullable = false)
    private boolean active = true; // 활성 여부

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NoticeStatus status = NoticeStatus.PUBLISHED; // 상태 (DRAFT, PUBLISHED, ARCHIVED)

    @Column(name = "createdat", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now(); // 등록일

    @Column(name = "updatedat")
    private LocalDateTime updatedAt; // 수정일

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum NoticeStatus {
        DRAFT, PUBLISHED, ARCHIVED
    }
}