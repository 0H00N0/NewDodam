package com.dodam.inquiry.dto;
import lombok.*;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

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
  
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
  private LocalDateTime answeredAt;
  
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
  private LocalDateTime createdAt;
}
