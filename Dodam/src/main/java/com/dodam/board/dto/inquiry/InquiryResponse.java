package com.dodam.board.dto.inquiry;
import lombok.*; import java.time.*;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InquiryResponse {
    private Long id; private String code; private String title; private String content;
    private String contactEmail; private boolean secret; private String status; private String answerContent;
    private LocalDateTime answeredAt; private LocalDateTime createdAt; private LocalDateTime updatedAt;
}