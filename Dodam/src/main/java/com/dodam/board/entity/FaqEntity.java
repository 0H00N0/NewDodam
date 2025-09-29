package com.dodam.board.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "faqs",
       indexes = { @Index(name="idx_faqs_board_code", columnList="boardcode"),
                   @Index(name="idx_faqs_category_sort", columnList="category, sortorder") })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FaqEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "boardcode", nullable = false)
    private BoardEntity board;
    @Column(length = 40)
    private String category;
    @Column(nullable = false, length = 200)
    private String question;
    @Lob @Column(nullable = false)
    private String answer;
    @Column(name="sortorder", nullable = false)
    private int sortOrder = 0;
    @Column(nullable = false)
    private boolean enabled = true;
    @Column(name="createdat", nullable = false, updatable = false)
    private java.time.LocalDateTime createdAt = java.time.LocalDateTime.now();
    @Column(name="updatedat")
    private java.time.LocalDateTime updatedAt;
    @PreUpdate void onUpdate() { this.updatedAt = java.time.LocalDateTime.now(); }
}