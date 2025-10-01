package com.dodam.cart.service;


import com.dodam.cart.Entity.CartEntity;
import com.dodam.cart.dto.CartDTO;
import com.dodam.cart.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {
    private final CartRepository cartRepo;

    public CartDTO get(Long cartnum) {
        CartEntity entity = cartRepo.findById(cartnum)
            .orElseThrow(() -> new RuntimeException("Cart not found: " + cartnum));
        return toDTO(entity);
    }

    public CartDTO save(CartDTO dto) {
        CartEntity entity = CartEntity.builder()
            .cartnum(dto.getCartnum())
            .mnum(dto.getMnum())
            .pronum(dto.getPronum())
            .catenum(dto.getCatenum())
            .build();
        entity = cartRepo.save(entity);
        return toDTO(entity);
    }

    private CartDTO toDTO(CartEntity entity) {
        return CartDTO.builder()
            .cartnum(entity.getCartnum())
            .mnum(entity.getMnum())
            .pronum(entity.getPronum())
            .catenum(entity.getCatenum())
            .build();
    }
}