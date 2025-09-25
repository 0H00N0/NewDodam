package com.dodam.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRequestDTO {
    private String evName;       // 이벤트 이름
    private String evContent;    // 이벤트 내용
    private Integer status;      // 0=예정, 1=진행중, 2=종료
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime startTime;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime endTime;
    private String eventType;
    private Integer capacity;   // ✅ 새로 추가: 선착순 이벤트 정원
    private Long lotTypeNum;  // 관리자 이벤트 생성 시 전달받음

}
