package com.dodam.board.dto.faq;
import lombok.*; import java.time.*;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FaqResponse {
    private Long id; private String boardCode; private String category; private String question; private String answer;
    private int sortOrder; private boolean enabled; private LocalDateTime createdAt; private LocalDateTime updatedAt;
}