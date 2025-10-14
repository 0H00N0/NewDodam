package com.dodam.rent.service;

import com.dodam.member.entity.MemberEntity;
import com.dodam.member.repository.MemberRepository;
import com.dodam.product.entity.ProductEntity;
import com.dodam.product.repository.ProductRepository;
import com.dodam.rent.entity.RentEntity;
import com.dodam.rent.repository.RentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RentService {
    private final RentRepository rentRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;

    @Transactional
    public RentEntity rentProduct(Long mnum, Long pronum) {
        // ✅ 네 파일 그대로 둠 (현재 SHIPPING 상태로 저장)
        MemberEntity member = memberRepository.findById(mnum)
            .orElseThrow(() -> new IllegalArgumentException("회원 정보 없음: " + mnum));
        ProductEntity product = productRepository.findById(pronum)
            .orElseThrow(() -> new IllegalArgumentException("상품 정보 없음: " + pronum));

        RentEntity rent = RentEntity.builder()
            .member(member)
            .product(product)
            .renDate(java.time.LocalDateTime.now())
            .renShip(RentEntity.ShipStatus.SHIPPING)
            .build();

        return rentRepository.save(rent);
    }

    // ✅ 추가: 내 주문목록 조회 (List<RentResponseDTO>)
    @Transactional(readOnly = true)
    public java.util.List<com.dodam.rent.dto.RentResponseDTO> findByMemberMid(String mid) {
        return rentRepository.findByMember_MidOrderByRenNumDesc(mid)
            .stream()
            .map(r -> {
                com.dodam.rent.dto.RentResponseDTO dto = new com.dodam.rent.dto.RentResponseDTO();
                dto.setRentNum(r.getRenNum());
                dto.setMnum(r.getMember().getMnum());
                dto.setPronum(r.getProduct().getPronum());
                dto.setProductName(r.getProduct().getProname());
                dto.setStatus(r.getRenShip() != null ? r.getRenShip().name() : null);
                dto.setRentDate(r.getRenDate() != null ? r.getRenDate().toString() : null);
                return dto;
            })
            .toList();
    }
}
