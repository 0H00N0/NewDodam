package com.dodam.product.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "productimage")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductImageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "proimagenum")
    private Long proimagenum;

    @Column(name = "proimageorder")
    private Integer proimageorder;

    @Column(name = "prourl", length = 400)
    private String prourl;               // 미리보기 URL

    @Column(name = "prodetailimage", length = 400)
    private String prodetailimage;       // 상세보기 URL

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pronum", nullable = false)
    private ProductEntity product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "catenum", nullable = false)
    private CategoryEntity category;
}
