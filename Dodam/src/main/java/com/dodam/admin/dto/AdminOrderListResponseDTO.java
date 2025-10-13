// src/main/java/com/dodam/admin/dto/AdminOrderListResponseDTO.java
package com.dodam.admin.dto;

import com.dodam.rent.entity.RentEntity;
import com.dodam.rent.entity.RentEntity.ShipStatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class AdminOrderListResponseDTO {

    private final Long renNum;            // 대여 PK
    private final String productName;     // 상품명
    private final String memberName;      // 주문자명
    private final LocalDateTime renDate;  // 대여일
    private final LocalDateTime retDate;  // 반납일
    private final ShipStatus renShip;     // 배송상태(ENUM: SHIPPING/DELIVERED)
    private final String trackingNumber;  // 운송장

    public AdminOrderListResponseDTO(RentEntity rent) {
        this.renNum = rent.getRenNum();
        this.productName = rent.getProduct() != null ? rent.getProduct().getProname() : null;
        this.memberName  = rent.getMember()  != null ? rent.getMember().getMname()   : null;
        this.renDate = rent.getRenDate();
        this.retDate = rent.getRetDate();
        this.renShip = rent.getRenShip();
        this.trackingNumber = rent.getTrackingNumber();
    }
}
