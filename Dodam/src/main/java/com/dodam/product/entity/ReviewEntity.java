package com.dodam.product.entity;


//import com.dodam.member.entity.MemberEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "review")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReviewEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "revnum")
    private Long revnum;

    @Column(name = "revscore", nullable = false)
    private Integer revscore; // 기본값 0은 DB default로 설정 권장

    @Column(name = "revtitle", length = 200)
    private String revtitle;

    @Column(name = "revtext", length = 2000)
    private String revtext;

    @Column(name = "revcre")
    private LocalDateTime revcre;

    @Column(name = "revupdate")
    private LocalDateTime revupdate;

    @Column(name = "revlike")
    private Long revlike;

    // ===== FK =====
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pronum", nullable = false)
    private ProductEntity product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "catenum", nullable = false)
    private CategoryEntity category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prosnum", nullable = false)
    private ProstateEntity prostate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "revstatenum", nullable = false)
    private ReviewStateEntity reviewState;

//    @ManyToOne(fetch = FetchType.LAZY, optional = false)
//    @JoinColumn(name = "mnum", nullable = false)
//    private MemberEntity member;
}
