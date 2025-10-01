package com.dodam.board.dto.inquiry;
import jakarta.validation.constraints.*; import lombok.*;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InquiryCreateRequest {
    @NotBlank @Size(max=40) private String code;
    @NotBlank @Size(max=200) private String title;
    @NotBlank private String content;
    @Email @Size(max=120) private String contactEmail;
    private boolean secret; @Size(max=120) private String secretPassword;
}