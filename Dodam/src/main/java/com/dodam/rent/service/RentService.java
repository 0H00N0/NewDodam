package com.dodam.rent.service;

import com.dodam.member.entity.MemberEntity;
import com.dodam.member.repository.MemberRepository;
import com.dodam.product.entity.ProductEntity;
import com.dodam.product.repository.ProductRepository;
import com.dodam.rent.dto.RentResponseDTO;
import com.dodam.rent.entity.RentEntity;
import com.dodam.rent.repository.RentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RentService {

    private final RentRepository rentRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;

    @Transactional
    public RentEntity rentProduct(Long mnum, Long pronum) {
        // ✅ 기존 주문 생성 로직 유지
        MemberEntity member = memberRepository.findById(mnum)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보 없음: " + mnum));
        ProductEntity product = productRepository.findById(pronum)
                .orElseThrow(() -> new IllegalArgumentException("상품 정보 없음: " + pronum));

        RentEntity rent = RentEntity.builder()
                .member(member)
                .product(product)
                .renDate(LocalDateTime.now())
                .renShip(RentEntity.ShipStatus.SHIPPING) // 기본 배송상태
                .build();

        return rentRepository.save(rent);
    }

    // ✅ 내 주문목록 조회 (변경 없음)
    @Transactional(readOnly = true)
    public List<RentResponseDTO> findByMemberMid(String mid) {
        return rentRepository.findByMember_MidOrderByRenNumDesc(mid)
                .stream()
                .map(r -> {
                    RentResponseDTO dto = new RentResponseDTO();
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

    // ---------------------------
    // 🔽🔽 신규 추가: 취소 / 교환 / 반품
    // ---------------------------

    /** 배송중(SHIPPING)일 때만 취소 허용 */
    @Transactional
    public void cancelRent(String mid, Long renNum) {
        RentEntity rent = rentRepository.findById(renNum)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "주문을 찾을 수 없음: " + renNum));

        // 본인 주문인지 확인
        String ownerMid = rent.getMember().getMid();
        if (ownerMid == null || !ownerMid.equals(mid)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 주문만 취소할 수 있습니다.");
        }

        if (rent.getRenShip() != RentEntity.ShipStatus.SHIPPING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "배송완료 후에는 취소할 수 없습니다.");
        }

        // ⚠️ 추가 컬럼 없이 구현: 레코드 삭제로 취소 처리 (운영에선 로그/이력 테이블 권장)
        rentRepository.delete(rent);
    }

    /** 배송중(SHIPPING)일 때만 교환 허용: 같은 주문의 상품 참조만 교체 */
    @Transactional
    public void exchangeRent(String mid, Long renNum, Long newPronum, String reason) {
        RentEntity rent = rentRepository.findById(renNum)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "주문을 찾을 수 없음: " + renNum));

        String ownerMid = rent.getMember().getMid();
        if (ownerMid == null || !ownerMid.equals(mid)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 주문만 교환할 수 있습니다.");
        }

        if (rent.getRenShip() != RentEntity.ShipStatus.SHIPPING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "배송중에만 교환할 수 있습니다.");
        }

        ProductEntity newProd = productRepository.findById(newPronum)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "교환 대상 상품이 존재하지 않습니다: " + newPronum));

        rent.setProduct(newProd);
        // 추가 컬럼이 없으므로 reason 등은 DB 저장 생략(서버 로그 정도로만 남길 수 있음)
    }

    /** 배송완료(DELIVERED)일 때만 반품 허용: 최소 기록으로 retDate에 접수시각 저장 */
    @Transactional
    public void returnRent(String mid, Long renNum, String reason) {
        RentEntity rent = rentRepository.findById(renNum)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "주문을 찾을 수 없음: " + renNum));

        String ownerMid = rent.getMember().getMid();
        if (ownerMid == null || !ownerMid.equals(mid)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 주문만 반품할 수 있습니다.");
        }

        if (rent.getRenShip() != RentEntity.ShipStatus.DELIVERED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "배송완료 후에만 반품할 수 있습니다.");
        }

        // 추가 컬럼 없이 최소한의 기록: 반품 접수 시각을 retDate에 저장(운영 규칙으로 의미 합의)
        rent.setRetDate(LocalDateTime.now());
    }
}
