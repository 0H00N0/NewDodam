package com.dodam.board.dto.event;
import lombok.*; import java.time.*;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventResponse {
    private Long id; private String boardCode; private String title; private String content;
    private LocalDate startDate; private LocalDate endDate; private String bannerUrl; private boolean active; private int views;
    private LocalDateTime createdAt; private LocalDateTime updatedAt;
}