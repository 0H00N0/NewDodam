package com.dodam.voc.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "VocAnswer") // 테이블명 PascalCase
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VocAnswerEntity {

    @Id
    @Column(name = "vocId") // PK이자 FK
    private Long vocId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "vocId") // 컬럼명 camelCase
    private VocEntity voc;

    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "createdAt", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updatedAt", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}