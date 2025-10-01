package com.dodam.board.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "events",
    indexes = {
        @Index(name = "idx_events_board_code", columnList = "boardcode"),
        @Index(name = "idx_events_active", columnList = "isactive")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "eid", nullable = false)
    private Long eid; // 이벤트 번호 (PK)

    // 이벤트가 속한 게시판 (FK)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "boardcode", nullable = false)
    private BoardPostEntity board; // Board 테이블 참조

    @Column(name = "etitle", nullable = false, length = 200)
    private String etitle; // 이벤트 제목

    @Lob
    @Column(name = "econtent", nullable = false)
    private String econtent; // 이벤트 상세 내용

    @Column(name = "startdate", nullable = false)
    private LocalDate startDate; // 시작일

    @Column(name = "enddate", nullable = false)
    private LocalDate endDate; // 종료일

    @Column(name = "bannerurl", length = 255)
    private String bannerUrl; // 배너 이미지 URL

    @Column(name = "isactive", nullable = false)
    private boolean isActive = true; // 이벤트 활성화 여부 (기본값 true)

    @Column(name = "views", nullable = false)
    private int views = 0; // 조회수 (기본값 0)

    @Column(name = "createdat", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now(); // 생성일

    @Column(name = "updatedat")
    private LocalDateTime updatedAt; // 수정일

    // --- 엔티티 이벤트 콜백 ---
    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    void validateDates() {
        if (endDate != null && startDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate는 startDate보다 빠를 수 없습니다.");
        }
    }
}