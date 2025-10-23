package com.dodam.product.service;

import com.dodam.member.entity.MemberEntity;
import com.dodam.member.repository.MemberRepository;
import com.dodam.product.dto.MyReviewDTO;
import com.dodam.product.entity.ProductEntity;
import com.dodam.product.entity.ReviewEntity;
import com.dodam.product.repository.ProductRepository;
import com.dodam.product.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;

    //상품별 리뷰목록 조회
    public List<MyReviewDTO> getReviewsByProduct(Long pronum) {
        ProductEntity product = productRepository.findById(pronum)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        return reviewRepository.findByProductOrderByRevcreDesc(product)
                .stream()
                .map(MyReviewDTO::from)
                .collect(Collectors.toList());
    }

    //리뷰 작성
    public void createReview(MyReviewDTO dto) {
        // 회원 조회
        MemberEntity member = memberRepository.findById(dto.getMnum())
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

        // 상품 조회
        ProductEntity product = productRepository.findById(dto.getPronum())
                .orElseThrow(() -> new IllegalArgumentException("상품 정보를 찾을 수 없습니다."));

        // 리뷰 엔티티 생성
        ReviewEntity review = new ReviewEntity();
        review.setMember(member);
        review.setProduct(product);
        review.setRevtitle(dto.getRevtitle());
        review.setRevtext(dto.getRevtext());
        review.setRevscore(dto.getRevscore());
        review.setRevcre(LocalDateTime.now());
        review.setRevupdate(LocalDateTime.now());

        reviewRepository.save(review);
    }

    //리뷰 수정
    public void updateReview(Long revnum, MyReviewDTO dto, Long mnum) {
        ReviewEntity review = reviewRepository.findById(revnum)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));

        // 수정 시 본인 확인
        if (!review.getMember().getMnum().equals(mnum)) {
            throw new IllegalArgumentException("본인 리뷰만 수정할 수 있습니다.");
        }
        review.setRevtitle(dto.getRevtitle());
        review.setRevtext(dto.getRevtext());
        review.setRevscore(dto.getRevscore());
        review.setRevupdate(LocalDateTime.now());
    }

    //리뷰 삭제
    public void deleteReview(Long revnum, Long mnum) {
        ReviewEntity review = reviewRepository.findById(revnum)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));
        reviewRepository.delete(review);
        
        //삭제 시 본인확인
        if (!review.getMember().getMnum().equals(mnum)) {
            throw new IllegalArgumentException("본인 리뷰만 삭제할 수 있습니다.");
        }
    }
}