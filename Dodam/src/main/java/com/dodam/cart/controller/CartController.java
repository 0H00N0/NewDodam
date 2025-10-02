package com.dodam.cart.controller;

import com.dodam.cart.dto.CartDTO;
import com.dodam.cart.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.dodam.member.service.MemberService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/cart")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;
    private final MemberService memberService;

    @GetMapping("/{cartnum}")
    public CartDTO get(@PathVariable("cartnum") Long cartnum) {
        return cartService.get(cartnum);
    }

    //장바구니 담기
    @PostMapping
    public CartDTO save(@RequestBody CartDTO dto, HttpSession session) {
        String sid = (String) session.getAttribute("sid");
        if (sid == null) throw new RuntimeException("로그인 정보가 없습니다.");

        Long mnum = memberService.findMnumByMid(sid);
        if (mnum == null) throw new RuntimeException("회원 정보가 없습니다.");

        dto.setMnum(mnum); // mnum 세팅
        return cartService.save(dto);
    }
}