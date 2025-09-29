package com.dodam.board.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inquiries",
       indexes = { @Index(name="idx_inquiries_board_code", columnList="boardcode"),
                   @Index(name="idx_inquiries_status",     columnList="status") })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InquiryEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "boardcode", nullable = false)
    private BoardEntity board;
    @Column(nullable = false, length = 200)
    private String title;
    @Lob @Column(nullable = false)
    private String content;
    @Column(length = 120)
    private String contactEmail;
    @Column(name="issecret", nullable = false)
    private boolean secret = false;
    @Column(name="secretpassword", length = 120)
    private String secretPassword;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InquiryStatus status = InquiryStatus.OPEN;
    @Lob
    private String answerContent;
    private java.time.LocalDateTime answeredAt;
    @Column(name="createdat", nullable = false, updatable = false)
    private java.time.LocalDateTime createdAt = java.time.LocalDateTime.now();
    @Column(name="updatedat")
    private java.time.LocalDateTime updatedAt;
    @PreUpdate void onUpdate() { this.updatedAt = java.time.LocalDateTime.now(); }
    public enum InquiryStatus { OPEN, ANSWERED, CLOSED }
}