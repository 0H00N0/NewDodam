// src/main/java/com/dodam/admin/dto/AdminOrderListResponseDTO.java
package com.dodam.admin.dto;

import com.dodam.rent.entity.RentEntity;
import com.dodam.rent.entity.RentEntity.ShipStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class AdminOrderListResponseDTO {

    // ➤ 프런트: o.renNum
    @JsonProperty("renNum")
    private final Long renNum;               // 대여 PK

    // ➤ 프런트: o.prodName
    @JsonProperty("prodName")
    private final String productName;        // 상품명

    // ➤ 프런트: o.mid  (화면 라벨은 "주문자명"이지만 키는 mid를 사용 중)
    @JsonProperty("mid")
    private final String memberName;         // 주문자명(또는 ID 대신 이름 사용)

    // ➤ 프런트: o.rentalDate
    @JsonProperty("rentalDate")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime renDate;     // 대여일

    // ➤ 프런트: o.returnDate
    @JsonProperty("returnDate")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime retDate;     // 반납일

    // ➤ 프런트: o.status (PENDING/SHIPPING/DELIVERED/RETURNED 중 하나의 문자열 기대)
    @JsonProperty("status")
    private final String status;             // 배송상태 문자열

    // ➤ 프런트: o.trackingNum
    @JsonProperty("trackingNum")
    private final String trackingNumber;     // 운송장

    public AdminOrderListResponseDTO(RentEntity rent) {
        this.renNum = rent.getRenNum();
        this.productName = rent.getProduct() != null ? rent.getProduct().getProname() : null;
        // 주문자명은 화면 요구대로 '이름'을 제공 (필요 시 ID로 교체 가능)
        this.memberName  = rent.getMember()  != null ? rent.getMember().getMname()   : null;

        this.renDate = rent.getRenDate();
        this.retDate = rent.getRetDate();

        // ENUM → 문자열(프런트 매핑표와 호환되도록 대문자 유지)
        ShipStatus ship = rent.getRenShip();
        this.status = ship != null ? ship.name() : "PENDING";

        this.trackingNumber = rent.getTrackingNumber();
    }
}
