package com.dodam.board.dto.notice;
import jakarta.validation.constraints.*; import lombok.*;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NoticeUpdateRequest {
    @Size(max=200) private String title;
    private String content; private Boolean pinned; private String status;
}