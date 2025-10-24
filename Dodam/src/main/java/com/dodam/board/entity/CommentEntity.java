package com.dodam.board.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "boardComment") // ✅ 안전한 이름으로 변경
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "comment_seq")
    @SequenceGenerator(
        name = "comment_seq",
        sequenceName = "COMMENT_SEQ", // 시퀀스명은 그대로 사용 가능
        allocationSize = 1
    )
    @Column(name = "CONUM", nullable = false)
    private Long conum;

    @Column(name = "MNUM", nullable = false)
    private Long mnum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BNUM", nullable = false) // BOARD(BNUM) 참조
    private BoardEntity board;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="PARENTCONUM")
    private CommentEntity parent;   // ✅ 부모 댓글

    @Column(name = "CCONTENT", length = 1000)
    private String ccontent;

    @Column(name = "MNIC", nullable = false, length = 255)
    private String mnic;

    @Column(name = "MID", nullable = false, length = 255)
    private String mid;
    
    @Column(name = "cdate")
    private LocalDateTime cdate;

    @Column(name = "cedate")
    private LocalDateTime cedate;

    @PrePersist
    public void onCreate() {
        this.cdate = LocalDateTime.now();
        this.cedate = this.cdate;
    }
    @PreUpdate
    public void onUpdate() {
        this.cedate = LocalDateTime.now();
    }
}
