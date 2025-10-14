package com.dodam.rent.controller;

import com.dodam.member.entity.MemberEntity;
import com.dodam.member.repository.MemberRepository;
import com.dodam.rent.dto.RentDTO;
import com.dodam.rent.dto.RentResponseDTO;
import com.dodam.rent.entity.RentEntity;
import com.dodam.rent.service.RentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/rent")
@RequiredArgsConstructor
public class RentController {
    private final RentService rentService;
    private final MemberRepository memberRepository;

    @PostMapping
    public RentResponseDTO rentProduct(@RequestBody RentDTO req, HttpSession session) {
        // 세션에서 mid(아이디) 꺼내기
        String mid = (String) session.getAttribute("sid");
        if (mid == null) {
            throw new IllegalArgumentException("로그인 정보가 없습니다.");
        }

        // member 테이블에서 mid로 mnum 조회
        MemberEntity member = memberRepository.findByMid(mid)
            .orElseThrow(() -> new IllegalArgumentException("회원 정보 없음: " + mid));
        Long mnum = member.getMnum();

        // pronum은 프론트에서 넘어온 값 사용
        RentEntity rent = rentService.rentProduct(mnum, req.getPronum());

        RentResponseDTO dto = new RentResponseDTO();
        dto.setMnum(rent.getMember().getMnum());
        dto.setPronum(rent.getProduct().getPronum());
        dto.setProductName(rent.getProduct().getProname());
        dto.setStatus(rent.getRenShip().name());
        dto.setRentDate(rent.getRenDate().toString());
        // 필요시 추가 정보 세팅

        return dto;
    }
    
    // ✅ [신규] 내 주문목록: GET /rent/my
    @GetMapping("/my")
    public java.util.List<com.dodam.rent.dto.RentResponseDTO> my(HttpSession session) {
        String sid = (String) session.getAttribute("sid");
        if (sid == null || sid.isBlank()) throw new RuntimeException("로그인 필요");

        // 계정 존재 확인(탈퇴/비활성 방지)
        memberRepository.findByMid(sid)
            .orElseThrow(() -> new IllegalArgumentException("회원 없음: " + sid));

        return rentService.findByMemberMid(sid);
    }
}