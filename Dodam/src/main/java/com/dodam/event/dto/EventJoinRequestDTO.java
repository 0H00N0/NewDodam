package com.dodam.event.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventJoinRequestDTO {
    private Long evNum;   // 이벤트 번호
    private Long mnum;    // 회원 번호
    private Long lotNum;  // 추첨권 번호 (추첨일 경우 필요)
}
