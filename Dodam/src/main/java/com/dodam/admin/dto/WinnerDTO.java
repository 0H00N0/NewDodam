package com.dodam.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class WinnerDTO {
    private Long mnum;
    private String mname;   // 이름
    private String memail;  // 이메일
}
