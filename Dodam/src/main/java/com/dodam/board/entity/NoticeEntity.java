package com.dodam.board.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notices",
       indexes = { @Index(name="idx_notices_board_code", columnList="board_code"),
                   @Index(name="idx_notices_pinned",     columnList="is_pinned") })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NoticeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "code", nullable = false)
    private BoardEntity code;
    @Column(nullable = false, length = 200)
    private String title;
    @Lob @Column(nullable = false)
    private String content;
    @Column(name="is_pinned", nullable = false)
    private boolean pinned = false;
    @Column(nullable = false)
    private int views = 0;
    @Column(nullable = false, length = 50)
    private String author;
    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NoticeStatus status = NoticeStatus.PUBLISHED;
    @Column(name="created_at", nullable = false, updatable = false)
    private java.time.LocalDateTime createdAt = java.time.LocalDateTime.now();
    @Column(name="updated_at")
    private java.time.LocalDateTime updatedAt;
    @PreUpdate void onUpdate() { this.updatedAt = java.time.LocalDateTime.now(); }
    public enum NoticeStatus { DRAFT, PUBLISHED, ARCHIVED }
}