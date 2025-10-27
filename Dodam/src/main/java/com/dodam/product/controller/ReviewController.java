package com.dodam.product.controller;

import com.dodam.member.entity.MemberEntity;
import com.dodam.member.repository.MemberRepository;
import com.dodam.product.dto.MyReviewDTO;
import com.dodam.product.entity.ProductEntity;
import com.dodam.product.entity.ReviewEntity;
import com.dodam.product.repository.ProductRepository;
import com.dodam.product.repository.ReviewRepository;
import com.dodam.product.service.ReviewService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final ReviewService reviewService;

    //  상품별 리뷰 목록
    @GetMapping("/{pronum}")
    public ResponseEntity<List<MyReviewDTO>> getReviewsByProduct(@PathVariable("pronum") Long pronum) {
        ProductEntity product = productRepository.findById(pronum)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        List<MyReviewDTO> list = reviewRepository.findByProductOrderByRevcreDesc(product).stream()
                .map(MyReviewDTO::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(list);
    }

    //  리뷰 작성
    @PostMapping
    public ResponseEntity<?> addReview(@RequestBody MyReviewDTO dto) {
        // member 조회: mnum 기준
        MemberEntity member = memberRepository.findById(dto.getMnum())
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));
        ProductEntity product = productRepository.findById(dto.getPronum())
                .orElseThrow(() -> new IllegalArgumentException("상품 정보를 찾을 수 없습니다."));

        ReviewEntity review = new ReviewEntity();
        review.setMember(member);
        review.setProduct(product);
        review.setRevtitle(dto.getRevtitle());
        review.setRevtext(dto.getRevtext());
        review.setRevscore(dto.getRevscore());
        review.setRevcre(LocalDateTime.now());

        reviewRepository.save(review);
        return ResponseEntity.ok("리뷰가 등록되었습니다.");
    }

    // 리뷰 수정
    @PutMapping("/{revnum}/{mnum}")
    public ResponseEntity<?> updateReview(@PathVariable Long revnum, @PathVariable Long mnum,
                                          @RequestBody MyReviewDTO dto) {
        reviewService.updateReview(revnum, dto, mnum);
        return ResponseEntity.ok("리뷰 수정 완료");
    }

    //  리뷰 삭제
    @DeleteMapping("/{revnum}/{mnum}")
    public ResponseEntity<?> deleteReview(@PathVariable Long revnum, @PathVariable Long mnum) {
        reviewService.deleteReview(revnum, mnum);
        return ResponseEntity.ok("리뷰 삭제 완료");
    }
}