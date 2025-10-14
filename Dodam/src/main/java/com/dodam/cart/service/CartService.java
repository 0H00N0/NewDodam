package com.dodam.cart.service;

import com.dodam.cart.Entity.CartEntity;
import com.dodam.cart.dto.CartDTO;
import com.dodam.cart.dto.CartItemViewDTO;
import com.dodam.cart.repository.CartRepository;
import com.dodam.product.entity.ProductEntity;
import com.dodam.product.repository.ProductRepository;
import com.dodam.rent.service.RentService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepo;
    private final ProductRepository productRepo;  // ✅ 상품명/가격 조회용
    private final RentService rentService;

    public CartDTO get(Long cartnum) {
        CartEntity entity = cartRepo.findById(cartnum)
        	.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found"));
        return toDTO(entity);
    }

    /**
     * ✅ 내 장바구니 목록 조회 (표시용 DTO)
     */
    @Transactional(readOnly = true)
    public List<CartItemViewDTO> findMy(Long mnum) {
        List<CartEntity> items = cartRepo.findByMnum(mnum);
        if (items.isEmpty()) return List.of();

        // 상품 정보 한번에 조인/매핑
        Set<Long> proIds = items.stream().map(CartEntity::getPronum).collect(Collectors.toSet());
        Map<Long, ProductEntity> prodMap = productRepo.findAllById(proIds).stream()
                .collect(Collectors.toMap(ProductEntity::getPronum, p -> p));

        List<CartItemViewDTO> result = new ArrayList<>();
        for (CartEntity e : items) {
            ProductEntity p = prodMap.get(e.getPronum());
            result.add(CartItemViewDTO.builder()
                    .cartnum(e.getCartnum())
                    .pronum(e.getPronum())
                    .proname(p != null ? p.getProname() : null)
                    .price(p != null ? p.getProprice() : null)    // 표시용 현재가
                    .thumbnail(null)                               // 필요시 이미지 조인해서 채워도 됨
                    .qty(e.getQty())                              // ✅ 여기! 1 고정 → e.getQty() 로 변경
                    .build());
        }
        return result;
    }

    /**
     * ✅ UPSERT: (mnum, pronum) 중복 방지
     */
    @Transactional
    public CartDTO upsert(CartDTO dto) {
        CartEntity exist = cartRepo.findByMnumAndPronum(dto.getMnum(), dto.getPronum()).orElse(null);
        if (exist != null) {
            exist.setQty(exist.getQty() + Math.max(1, dto.getQty())); // 누적
            return toDTO(exist);
        }
        CartEntity entity = CartEntity.builder()
                .mnum(dto.getMnum())
                .pronum(dto.getPronum())
                .catenum(dto.getCatenum())
                .qty(Math.max(1, dto.getQty()))
                .build();
        cartRepo.save(entity);
        return toDTO(entity);
    }


    /**
     * (필요 시 유지) 단순 save — 이제는 upsert로 위임
     */
    @Transactional
    public CartDTO save(CartDTO dto) {
        return upsert(dto);
    }

    private CartDTO toDTO(CartEntity entity) {
        return CartDTO.builder()
            .cartnum(entity.getCartnum())
            .mnum(entity.getMnum())
            .pronum(entity.getPronum())
            .catenum(entity.getCatenum())
            .qty(entity.getQty())
            .build();
    }
    
    /** 수량 변경 */
    @Transactional 
    public void changeQty(Long mnum, Long pronum, int qty) {
        if (qty < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "qty >= 1");
        var e = cartRepo.findByMnumAndPronum(mnum, pronum)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "cart item"));
        e.setQty(qty);
        cartRepo.save(e);
    }

    /** 삭제 */
    @Transactional
    public void removeItem(Long mnum, Long pronum) {
        cartRepo.deleteByMnumAndPronum(mnum, pronum);
    }

    /** 체크아웃 → Rent 생성 (선택/전체) */
    public List<Long> checkoutToRent(Long mnum, List<Long> pronums, boolean clearCart) {
        var list = cartRepo.findByMnum(mnum);
        var targets = (pronums == null || pronums.isEmpty())
                ? list
                : list.stream().filter(e -> pronums.contains(e.getPronum())).toList();

        List<Long> rentIds = new ArrayList<>();
        for (var item : targets) {
            int times = Math.max(1, item.getQty());
            for (int i = 0; i < times; i++) {
                var rent = rentService.rentProduct(mnum, item.getPronum());
                // RentEntity PK 이름은 실제 코드에 맞게
                // 예: rent.getRenid()
                rentIds.add(rent.getRenNum());
            }
        }

        if (clearCart) {
            if (pronums == null || pronums.isEmpty()) cartRepo.deleteByMnum(mnum);
            else pronums.forEach(pr -> cartRepo.deleteByMnumAndPronum(mnum, pr));
        }
        return rentIds;
    }
}
