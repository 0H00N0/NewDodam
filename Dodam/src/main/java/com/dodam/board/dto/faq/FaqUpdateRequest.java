package com.dodam.board.dto.faq;
import jakarta.validation.constraints.*; import lombok.*;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FaqUpdateRequest {
    @Size(max=40) private String category; private String question; private String answer; private Integer sortOrder; private Boolean enabled;
}