package com.dodam.board.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "events", // ✅ 소문자
    indexes = {
        @Index(name = "idx_events_board_bnum", columnList = "bnum"),
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
    @SequenceGenerator(name = "ev_seq", sequenceName = "ev_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ev_seq")
    @Column(name = "eid", nullable = false)
    private Long eid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bnum", nullable = false, referencedColumnName = "bnum") // ✅ FK도 소문자
    private BoardEntity board;

    @Column(name = "etitle", nullable = false, length = 200)
    private String etitle;

    @Lob
    @Column(name = "econtent", nullable = false)
    private String econtent;

    @Column(name = "startdate", nullable = false)
    private LocalDate startDate;

    @Column(name = "enddate", nullable = false)
    private LocalDate endDate;

    @Column(name = "bannerurl", length = 255)
    private String bannerUrl;

    @Column(name = "isactive", nullable = false)
    private boolean isActive = true;

    @Column(name = "views", nullable = false)
    private int views = 0;

    @Column(name = "createdat", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updatedat")
    private LocalDateTime updatedAt;

    @PreUpdate
    void onUpdate() { this.updatedAt = LocalDateTime.now(); }

    @PrePersist
    void validateDates() {
        if (endDate != null && startDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate는 startDate보다 빠를 수 없습니다.");
        }
    }
}
