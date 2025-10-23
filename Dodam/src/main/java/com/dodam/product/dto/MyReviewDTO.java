package com.dodam.product.dto;

import com.dodam.product.entity.ReviewEntity;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MyReviewDTO {
    private Long revnum;
    private Integer revscore;
    private String revtitle;
    private String revtext;
    private LocalDateTime revcre;
    private LocalDateTime revupdate;
    private Long revlike;
    private Long mnum;		//작성자 정보
    private Long pronum;
    private String proname;   // ProductEntity에 맞춤
    private Long revstatenum;
    private String revstate;

    public static MyReviewDTO from(ReviewEntity e) {
        return MyReviewDTO.builder()
            .revnum(e.getRevnum())
            .revscore(e.getRevscore())
            .revtitle(e.getRevtitle())
            .revtext(e.getRevtext())
            .revcre(e.getRevcre())
            .revupdate(e.getRevupdate())
            .revlike(e.getRevlike())
            .mnum(e.getMember() != null ? e.getMember().getMnum() : null)
            .pronum(e.getProduct() != null ? e.getProduct().getPronum() : null)
            .proname(e.getProduct() != null ? e.getProduct().getProname() : null) // 필요 시 필드명 조정
            .revstatenum(e.getReviewState() != null ? e.getReviewState().getRevstatenum() : null)
            .revstate(e.getReviewState() != null ? e.getReviewState().getRevstate() : null)
            .build();
    }
}
