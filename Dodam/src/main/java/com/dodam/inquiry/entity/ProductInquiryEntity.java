package com.dodam.inquiry.entity;

import com.dodam.member.entity.MemberEntity;
import com.dodam.product.entity.ProductEntity;
import com.dodam.rent.entity.RentEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_inquiry",
       indexes = {
         @Index(name="idx_pi_member", columnList="mnum"),
         @Index(name="idx_pi_product", columnList="pronum"),
         @Index(name="idx_pi_status", columnList="status")
       })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductInquiryEntity {

  public enum Status { OPEN, ANSWERED, CLOSED }

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "mnum", nullable = false)
  private MemberEntity member;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "pronum", nullable = false)
  private ProductEntity product;

  // 어떤 주문(대여)에 대한 문의인지 추적용(필수는 아님)
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "rennum")
  private RentEntity rent;

  @Column(nullable = false, length = 200)
  private String title;

  @Lob
  @Column(nullable = false)
  private String content;

  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status = Status.OPEN;

  // 관리자 답변
  @Lob
  private String answerContent;

  private LocalDateTime answeredAt;

  @Builder.Default
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt = LocalDateTime.now();

  private LocalDateTime updatedAt;

  @PrePersist
  void onInsert() {
    if (createdAt == null) createdAt = LocalDateTime.now();
    if (updatedAt == null) updatedAt = createdAt;
    if (status == null) status = Status.OPEN;
  }

  @PreUpdate
  void onUpdate(){ this.updatedAt = LocalDateTime.now(); }
}
