package com.dodam.board.dto.faq;
import jakarta.validation.constraints.*; import lombok.*;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FaqCreateRequest {
    @NotBlank @Size(max=40) private String boardCode; @Size(max=40) private String category;
    @NotBlank @Size(max=200) private String question; @NotBlank private String answer;
    private Integer sortOrder; private Boolean enabled;
}