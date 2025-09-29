package com.dodam.board.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "events",
       indexes = { @Index(name="idx_events_board_code", columnList="boardcode"),
                   @Index(name="idx_events_active",     columnList="isactive") })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "boardcode", nullable = false)
    private BoardEntity board;
    @Column(nullable = false, length = 200)
    private String title;
    @Lob @Column(nullable = false)
    private String content;
    @Column(name="startdate", nullable = false)
    private java.time.LocalDate startDate;
    @Column(name="enddate", nullable = false)
    private java.time.LocalDate endDate;
    @Column(name="bannerurl", length = 255)
    private String bannerUrl;
    @Column(name="isactive", nullable = false)
    private boolean active = true;
    @Column(nullable = false)
    private int views = 0;
    @Column(name="createdat", nullable = false, updatable = false)
    private java.time.LocalDateTime createdAt = java.time.LocalDateTime.now();
    @Column(name="updated_at")
    private java.time.LocalDateTime updatedAt;
    @PreUpdate void onUpdate() { this.updatedAt = java.time.LocalDateTime.now(); }
    @PrePersist 
    void validateDates() {
        if (endDate != null && startDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate는 startDate보다 빠를 수 없습니다.");
        }
    }
}