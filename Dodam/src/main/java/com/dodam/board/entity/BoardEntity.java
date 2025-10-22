package com.dodam.board.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "board") // ✅ 소문자
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bnum", nullable = false) // ✅ 소문자
    private Long bnum;

    @Column(name = "mnum", nullable = false)
    private Long mnum;

    @Column(name = "mtnum", nullable = false)
    private Long mtnum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bcanum", nullable = false) // ✅ FK도 소문자
    private BoardCategoryEntity boardCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bsnum", nullable = true)
    private BoardStateEntity boardState;

    @Column(name = "bsub", length = 255)
    private String bsub;

    @Column(name = "bcontent", length = 4000)
    private String bcontent;

    @Column(name = "bdate")
    private LocalDateTime bdate;

    @Column(name = "bedate")
    private LocalDateTime bedate;

    @Column(name = "mid", nullable = false, length = 255)
    private String mid;

    @Column(name = "mnic", nullable = false, length = 255)
    private String mnic;
    
 // ✅ 자동 날짜 세팅
    @PrePersist
    public void onCreate() {
        this.bdate = LocalDateTime.now();
        this.bedate = LocalDateTime.now(); // 수정일도 초기값
    }

    @PreUpdate
    public void onUpdate() {
        this.bedate = LocalDateTime.now();
    }
}