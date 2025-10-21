package com.dodam.member.controller;

import com.dodam.member.entity.MemberEntity;
import com.dodam.member.repository.MemberRepository;
import com.dodam.product.dto.MyReviewDTO;
import com.dodam.product.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/member/reviews")
public class MemberReviewController {

    private final MemberRepository memberRepo;
    private final ReviewRepository reviewRepo;

    @GetMapping("/my")
    public ResponseEntity<?> my(HttpSession session) {
        String mid = (String) session.getAttribute("sid");
        if (mid == null || mid.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                  .body(Map.of("error","LOGIN_REQUIRED"));
        }

        MemberEntity me = memberRepo.findByMid(mid).orElse(null);
        if (me == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                  .body(Map.of("error","LOGIN_REQUIRED"));
        }

        List<MyReviewDTO> list = reviewRepo.findAllByMemberOrderByRevnumDesc(me)
                                           .stream().map(MyReviewDTO::from).toList();
        return ResponseEntity.ok(list);
    }
}
