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
import java.util.Map;

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
    
    /** 수량 변경 */
    @PatchMapping("/items/{pronum}")
    public void changeQty(@PathVariable("pronum") Long pronum,
                          @RequestBody Map<String, Integer> body,
                          HttpSession session) {
        Integer qty = body.get("qty");
        if (qty == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "qty required");
        cartService.changeQty(getMnum(session), pronum, qty);
    }

    /** 삭제 */
    @DeleteMapping("/items/{pronum}")
    public void remove(@PathVariable("pronum") Long pronum, HttpSession session) {
        cartService.removeItem(getMnum(session), pronum);
    }

    /** 체크아웃: 장바구니 → Rent */
    @PostMapping("/checkout")
    public Map<String, Object> checkout(@RequestBody(required = false) Map<String, Object> body,
                                        HttpSession session) {
        Long mnum = getMnum(session);
        List<Integer> p = body != null ? (List<Integer>) body.getOrDefault("pronums", List.of()) : List.of();
        boolean clear = body != null && Boolean.TRUE.equals(body.get("clearCart"));
        var rentIds = cartService.checkoutToRent(mnum, p.stream().map(Long::valueOf).toList(), clear);
        return Map.of("ok", true, "count", rentIds.size(), "rentIds", rentIds);
    }
    
}
