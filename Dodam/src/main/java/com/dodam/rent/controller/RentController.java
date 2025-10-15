package com.dodam.rent.controller;

import com.dodam.member.entity.MemberEntity;
import com.dodam.member.repository.MemberRepository;
import com.dodam.rent.dto.ExchangeRequestDTO;
import com.dodam.rent.dto.RentDTO;
import com.dodam.rent.dto.RentResponseDTO;
import com.dodam.rent.entity.RentEntity;
import com.dodam.rent.service.RentService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rent")
@RequiredArgsConstructor
public class RentController {

    private final RentService rentService;
    private final MemberRepository memberRepository;

    @PostMapping
    public RentResponseDTO rentProduct(@RequestBody RentDTO req, HttpSession session) {
        String mid = (String) session.getAttribute("sid");
        if (mid == null) throw new IllegalArgumentException("로그인 정보가 없습니다.");

        MemberEntity member = memberRepository.findByMid(mid)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보 없음: " + mid));
        Long mnum = member.getMnum();

        RentEntity rent = rentService.rentProduct(mnum, req.getPronum());

        RentResponseDTO dto = new RentResponseDTO();
        dto.setMnum(rent.getMember().getMnum());
        dto.setPronum(rent.getProduct().getPronum());
        dto.setProductName(rent.getProduct().getProname());
        dto.setStatus(rent.getRenShip().name());
        dto.setRentDate(rent.getRenDate().toString());
        return dto;
    }

    // ✅ 내 주문목록
    @GetMapping("/my")
    public List<RentResponseDTO> my(HttpSession session) {
        String sid = (String) session.getAttribute("sid");
        if (sid == null || sid.isBlank()) throw new RuntimeException("로그인 필요");

        memberRepository.findByMid(sid)
                .orElseThrow(() -> new IllegalArgumentException("회원 없음: " + sid));

        return rentService.findByMemberMid(sid);
    }

    // ---------------------------
    // 🔽🔽 신규 추가: 취소 / 교환 / 반품
    // ---------------------------

    /** 배송중(SHIPPING)일 때만 취소 허용 */
    @PostMapping("/{renNum}/cancel")
    public ResponseEntity<Map<String, Object>> cancel(@PathVariable("renNum") Long renNum, HttpSession session) {
        String sid = (String) session.getAttribute("sid");
        if (sid == null || sid.isBlank()) return ResponseEntity.status(401).build();

        rentService.cancelRent(sid, renNum);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    /** 배송중(SHIPPING)일 때만 교환 허용 */
    @PostMapping("/{renNum}/exchange")
    public ResponseEntity<Map<String, Object>> exchange(@PathVariable("renNum") Long renNum,
                                                        @RequestBody ExchangeRequestDTO req,
                                                        HttpSession session) {
        String sid = (String) session.getAttribute("sid");
        if (sid == null || sid.isBlank()) return ResponseEntity.status(401).build();

        rentService.exchangeRent(sid, renNum, req.getNewPronum(), req.getReason());
        return ResponseEntity.ok(Map.of("ok", true));
    }

    /** 배송완료(DELIVERED)일 때만 반품 허용 */
    @PostMapping("/{renNum}/return")
    public ResponseEntity<Map<String, Object>> requestReturn(@PathVariable("renNum") Long renNum,
                                                             @RequestBody(required = false) Map<String, String> body,
                                                             HttpSession session) {
        String sid = (String) session.getAttribute("sid");
        if (sid == null || sid.isBlank()) return ResponseEntity.status(401).build();

        String reason = (body != null) ? body.getOrDefault("reason", "") : "";
        rentService.returnRent(sid, renNum, reason);
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
