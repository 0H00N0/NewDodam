package com.dodam.board.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "faqs",
    indexes = {
        @Index(name="idx_faqs_board_code", columnList="boardcode"),
        @Index(name="idx_faqs_category_sort", columnList="category, sortorder")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaqEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // FAQ 번호 (PK)

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "boardcode", nullable = false)
    private BoardEntity board; // 게시판 코드 (FK)

    @Column(name = "category", length = 40, nullable = false)
    private String category; // 카테고리

    @Column(name = "question", nullable = false, length = 200)
    private String question; // 질문

    @Lob
    @Column(name = "answer", nullable = false)
    private String answer; // 답변

    @Column(name = "sortorder", nullable = false)
    private int sortOrder = 0; // 정렬 순서

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true; // 사용 여부

    @Column(name = "createdat", nullable = false, updatable = false)
    private java.time.LocalDateTime createdAt = java.time.LocalDateTime.now(); // 등록일

    @Column(name = "updatedat")
    private java.time.LocalDateTime updatedAt; // 수정일

    @PreUpdate
    void onUpdate() {
        this.updatedAt = java.time.LocalDateTime.now();
    }
}