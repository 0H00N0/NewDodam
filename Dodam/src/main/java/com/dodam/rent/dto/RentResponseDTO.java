package com.dodam.rent.dto;

import lombok.Data;

@Data
public class RentResponseDTO {
	private Long rentNum;         // 대여 번호
    private Long mnum;            // 회원 번호
    private Long pronum;          // 상품 번호
    private String productName;   // 상품명
    private String memberName;    // 회원명 (필요시)
    private String status;        // 대여 상태
    private String rentDate; 
}