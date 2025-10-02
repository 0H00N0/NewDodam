package com.dodam.cart.controller;

import com.dodam.cart.dto.CartDTO;
import com.dodam.cart.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;

    @GetMapping("/{cartnum}")
    public CartDTO get(@PathVariable("cartnum") Long cartnum) {
        return cartService.get(cartnum);
    }

    @PostMapping
    public CartDTO save(@RequestBody CartDTO dto) {
        return cartService.save(dto);
    }
}