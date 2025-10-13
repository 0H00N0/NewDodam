package com.dodam.cart.controller;

import com.dodam.cart.dto.CartDTO;
import com.dodam.cart.dto.CartItemViewDTO;
import com.dodam.cart.service.CartService;
import com.dodam.member.service.MemberService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/cart")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final MemberService memberService;

    private Long getMnum(HttpSession session) {
        String sid = (String) session.getAttribute("sid");
        if (sid == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 필요");
        Long mnum = memberService.findMnumByMid(sid);
        if (mnum == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "회원 없음");
        return mnum;
    }

    @GetMapping("/{cartnum}")
    public CartDTO get(@PathVariable("cartnum") Long cartnum) {
        return cartService.get(cartnum);
    }

    /** ✅ 내 장바구니 목록 (헤더/장바구니 페이지가 이걸 조회) */
    @GetMapping("/my")
    public List<CartItemViewDTO> my(HttpSession session) {
        return cartService.findMy(getMnum(session));
    }

    /** ✅ UPSERT로 담기 (권장 엔드포인트) */
    @PostMapping("/items")
    public CartDTO addItem(@RequestBody CartDTO dto, HttpSession session) {
        dto.setMnum(getMnum(session));
        return cartService.upsert(dto);
    }

    /** (호환 유지) 기존 POST /cart -> 내부적으로 UPSERT 수행 */
    @PostMapping
    public CartDTO save(@RequestBody CartDTO dto, HttpSession session) {
        dto.setMnum(getMnum(session));
        return cartService.upsert(dto);
    }
}
