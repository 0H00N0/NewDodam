package com.dodam.board.dto.inquiry;
import jakarta.validation.constraints.*; import lombok.*;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InquiryUpdateRequest {
    @Size(max=200) private String title; private String content; private String answerContent; private String status;
}