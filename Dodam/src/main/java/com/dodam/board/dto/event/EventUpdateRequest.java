package com.dodam.board.dto.event;
import lombok.*; import java.time.*;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventUpdateRequest {
    private String title; private String content; private LocalDate startDate; private LocalDate endDate; private String bannerUrl; private Boolean active;
}