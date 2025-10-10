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
        MemberEntity member = memberRepository.findById(mnum)
            .orElseThrow(() -> new IllegalArgumentException("회원 정보 없음: " + mnum));
        ProductEntity product = productRepository.findById(pronum)
            .orElseThrow(() -> new IllegalArgumentException("상품 정보 없음: " + pronum));

        RentEntity rent = RentEntity.builder()
            .member(member)
            .product(product)
            .renDate(LocalDateTime.now())
            .renShip(RentEntity.ShipStatus.SHIPPING)
            .build();

        return rentRepository.save(rent);
    }
}