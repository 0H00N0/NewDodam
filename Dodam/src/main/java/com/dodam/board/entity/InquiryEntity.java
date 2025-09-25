package com.dodam.board.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inquiries",
       indexes = { @Index(name="idx_inquiries_board_code", columnList="board_code"),
                   @Index(name="idx_inquiries_status",     columnList="status") })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InquiryEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "board_code", nullable = false)
    private BoardEntity board;
    @Column(nullable = false, length = 200)
    private String title;
    @Lob @Column(nullable = false)
    private String content;
    @Column(length = 120)
    private String contactEmail;
    @Column(name="is_secret", nullable = false)
    private boolean secret = false;
    @Column(name="secret_password", length = 120)
    private String secretPassword;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InquiryStatus status = InquiryStatus.OPEN;
    @Lob
    private String answerContent;
    private java.time.LocalDateTime answeredAt;
    @Column(name="created_at", nullable = false, updatable = false)
    private java.time.LocalDateTime createdAt = java.time.LocalDateTime.now();
    @Column(name="updated_at")
    private java.time.LocalDateTime updatedAt;
    @PreUpdate void onUpdate() { this.updatedAt = java.time.LocalDateTime.now(); }
    public enum InquiryStatus { OPEN, ANSWERED, CLOSED }
}