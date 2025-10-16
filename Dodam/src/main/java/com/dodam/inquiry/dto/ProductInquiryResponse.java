package com.dodam.inquiry.dto;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductInquiryResponse {
  private Long id;
  private Long pronum;
  private String productName;
  private Long renNum;
  private String title;
  private String content;
  private String status;          // OPEN/ANSWERED/CLOSED
  private String answerContent;   // nullable
  private LocalDateTime answeredAt;
  private LocalDateTime createdAt;
}
