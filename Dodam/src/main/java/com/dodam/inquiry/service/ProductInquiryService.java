package com.dodam.inquiry.service;

import com.dodam.inquiry.dto.*;
import com.dodam.inquiry.entity.ProductInquiryEntity;
import com.dodam.inquiry.repository.ProductInquiryRepository;
import com.dodam.member.entity.MemberEntity;
import com.dodam.member.repository.MemberRepository;
import com.dodam.product.entity.ProductEntity;
import com.dodam.product.repository.ProductRepository;
import com.dodam.rent.entity.RentEntity;
import com.dodam.rent.repository.RentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service @RequiredArgsConstructor
public class ProductInquiryService {

  private final ProductInquiryRepository repo;
  private final MemberRepository memberRepo;
  private final ProductRepository productRepo;
  private final RentRepository rentRepo;

  @Transactional
  public ProductInquiryResponse create(String mid, ProductInquiryCreateRequest req) {
    MemberEntity m = memberRepo.findByMid(mid)
        .orElseThrow(() -> new IllegalArgumentException("회원 없음: " + mid));
    ProductEntity p = productRepo.findById(req.getPronum())
        .orElseThrow(() -> new IllegalArgumentException("상품 없음: " + req.getPronum()));

    RentEntity rent = null;
    if (req.getRenNum() != null) {
      rent = rentRepo.findByRenNumWithJoins(req.getRenNum())
          .orElseThrow(() -> new IllegalArgumentException("주문 없음: " + req.getRenNum()));
      if (!rent.getMember().getMid().equals(mid))
        throw new IllegalArgumentException("내 주문이 아닙니다.");
      if (rent.getRenShip() != RentEntity.ShipStatus.DELIVERED)
        throw new IllegalArgumentException("배송완료된 주문만 문의할 수 있습니다.");
      if (!rent.getProduct().getPronum().equals(req.getPronum()))
        throw new IllegalArgumentException("주문 상품과 문의 상품이 일치하지 않습니다.");
    } else {
      // renNum 미지정 시: 해당 회원이 해당 상품을 'DELIVERED'로 받은 주문이 존재하는지 검증
      boolean hasDelivered = rentRepo.findByMember_MidOrderByRenNumDesc(mid).stream()
          .anyMatch(r -> r.getRenShip() == RentEntity.ShipStatus.DELIVERED
                      && r.getProduct().getPronum().equals(req.getPronum()));
      if (!hasDelivered)
        throw new IllegalArgumentException("이 상품은 배송완료 이력이 없습니다.");
    }

    ProductInquiryEntity ent = ProductInquiryEntity.builder()
        .member(m).product(p).rent(rent)
        .title(req.getTitle()).content(req.getContent())
        .status(ProductInquiryEntity.Status.OPEN)
        .build();

    ent = repo.save(ent);
    return toDto(ent);
  }

  @Transactional(readOnly = true)
  public List<ProductInquiryResponse> my(String mid) {
    return repo.findByMember_MidOrderByIdDesc(mid).stream().map(this::toDto).toList();
  }

  private ProductInquiryResponse toDto(ProductInquiryEntity e) {
    return ProductInquiryResponse.builder()
        .id(e.getId())
        .pronum(e.getProduct().getPronum())
        .productName(e.getProduct().getProname())
        .renNum(e.getRent() == null ? null : e.getRent().getRenNum())
        .title(e.getTitle())
        .content(e.getContent())
        .status(e.getStatus().name())
        .answerContent(e.getAnswerContent())
        .answeredAt(e.getAnsweredAt())
        .createdAt(e.getCreatedAt())
        .build();
  }
}
