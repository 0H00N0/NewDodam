package com.dodam.inquiry.dto;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductInquiryCreateRequest {
  @NotNull private Long pronum;          // 필수
  private Long renNum;                   // 있으면 검증 강화
  @NotBlank @Size(max=200) private String title;
  @NotBlank private String content;
}
