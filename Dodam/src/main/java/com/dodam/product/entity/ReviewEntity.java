package com.dodam.product.entity;

import com.dodam.member.entity.MemberEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "review",
  indexes = {
    @Index(name="idx_review_member", columnList="mnum"),
    @Index(name="idx_review_product", columnList="pronum"),
    @Index(name="idx_review_state", columnList="revstatenum")
  })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "revnum")
    private Long revnum;                  // PK

    @Column(name = "revscore", nullable = false)
    private Integer revscore;             // 평점 (기본 0)

    @Column(name = "revtitle")
    private String revtitle;              // 제목

    @Column(name = "revtext")
    private String revtext;               // 내용

    @Column(name = "revcre")
    private LocalDateTime revcre;         // 생성일시

    @Column(name = "revupdate")
    private LocalDateTime revupdate;      // 수정일시

    @Column(name = "revlike")
    private Long revlike;                 // 리뷰좋아요

    // FK 매핑
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pronum", nullable = false)
    private ProductEntity product;        // 상품

    @Column(name = "catenum", nullable = false)
    private Long catenum;                 // 카테고리번호

    @Column(name = "prostatus", nullable = false)
    private Long prostatus;               // 상품상태번호

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "revstatenum", nullable = false)
    private ReviewStateEntity reviewState; // 공개여부

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mnum", nullable = false)
    private MemberEntity member;          // 회원

    @PrePersist
    void onCreate() {
        if (revcre == null) revcre = LocalDateTime.now();
        if (revscore == null) revscore = 0;
        if (revlike == null) revlike = 0L;
    }
    @PreUpdate
    void onUpdate() { revupdate = LocalDateTime.now(); }
}
