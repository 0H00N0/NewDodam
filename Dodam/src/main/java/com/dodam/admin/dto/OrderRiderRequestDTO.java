package com.dodam.admin.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderRiderRequestDTO { //라이더 배정 dto
    private String renRider;
    private String trackingNumber;
}