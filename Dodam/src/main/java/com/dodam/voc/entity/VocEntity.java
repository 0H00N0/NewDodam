package com.dodam.voc.entity;

import com.dodam.member.entity.MemberEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Voc") // 테이블명 PascalCase
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VocEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vocId") // 컬럼명 camelCase
    private Long vocId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "authorMnum", referencedColumnName = "mnum", nullable = false)
    private MemberEntity author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handlerMnum", referencedColumnName = "mnum", nullable = true)
    private MemberEntity handler;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private VocStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private VocCategory category;

    @Column(name = "createdAt", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updatedAt", nullable = false)
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "voc", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private VocAnswerEntity answer;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        if (this.status == null) {
            this.status = VocStatus.RECEIVED;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // --- Enums ---
    public enum VocStatus { RECEIVED, IN_PROGRESS, COMPLETED }
    public enum VocCategory { PRODUCT, DELIVERY, ACCOUNT, SUGGESTION, ETC }
}