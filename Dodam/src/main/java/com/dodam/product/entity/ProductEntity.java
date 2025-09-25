package com.dodam.product.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "product")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pronum")
    private Long pronum; // PK

    @Column(name = "proname", length = 200)
    private String proname;

    @Column(name = "prodetail", length = 2000)
    private String prodetail;

    @Column(name = "proprice", precision = 15, scale = 2)
    private BigDecimal proprice;

    @Column(name = "proborrow", precision = 15, scale = 2)
    private BigDecimal proborrow;

    @Column(name = "probrand", length = 100)
    private String probrand;

    @Column(name = "promade", length = 100)
    private String promade;

    @Column(name = "proage")
    private Integer proage;

    @Column(name = "procertif", length = 100)
    private String procertif;

    @Column(name = "prodate")
    private LocalDate prodate;

    // 정의서: date 표기지만 생성/수정 "일시" 용도 → LocalDateTime 권장
    @Column(name = "procre")
    private LocalDateTime procre;

    @Column(name = "proupdate")
    private LocalDateTime proupdate;

    // ===== FK =====
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "catenum", nullable = false)
    private CategoryEntity category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prosnum", nullable = false)
    private ProstateEntity prostate;

    // 정의서상 FK지만 대상 테이블 스펙 미제공 → 우선 스칼라 보유
    @Column(name = "resernum", nullable = false)
    private Long resernum;

    @Column(name = "ctnum", nullable = false)
    private Long ctnum;
    
 // ==== 이미지 양방향 ====
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("proimageorder ASC")
    private List<ProductImageEntity> images = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (this.procre == null) this.procre = LocalDateTime.now();
        this.proupdate = this.procre;
    }

    @PreUpdate
    void onUpdate() {
        this.proupdate = LocalDateTime.now();
    }
}
