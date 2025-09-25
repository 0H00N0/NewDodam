package com.dodam.admin.dto;

import com.dodam.rent.entity.RentEntity;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class AdminOrderListResponseDTO {

    private Long renNum; // 주문(대여) 번호
    private String productName; // 상품명
    private String memberName; // 주문자명 (MemberEntity에 getName()이 있다고 가정)
    private LocalDateTime renDate; // 주문 일시
    private Integer renApproval; // 승인 상태 (0: 대기, 1: 승인, 2: 거절 등)
    private String renRider; // 배송 기사
    private String trackingNumber; // 운송장 번호

    public AdminOrderListResponseDTO(RentEntity rent) {
        this.renNum = rent.getRenNum();
        this.productName = rent.getProduct().getProname();
        // ※ MemberEntity에 'name' 필드와 getName() 메서드가 있어야 합니다.
        // 만약 필드명이 다르다면 이 부분을 수정해주세요. (예: rent.getMember().getUserId())
        this.memberName = rent.getMember().getMname();
        this.renDate = rent.getRenDate();
        this.renApproval = rent.getRenApproval();
        this.renRider = rent.getRenRider();
        this.trackingNumber = rent.getTrackingNumber();
    }
}