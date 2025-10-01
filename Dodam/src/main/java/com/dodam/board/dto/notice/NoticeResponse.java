package com.dodam.board.dto.notice;
import lombok.*; import java.time.*;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NoticeResponse {
    private Long id; private String code; private String title; private String content;
    private boolean pinned; private int views; private String status; private LocalDateTime createdAt; private LocalDateTime updatedAt;
}