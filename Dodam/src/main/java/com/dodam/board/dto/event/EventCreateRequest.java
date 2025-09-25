package com.dodam.board.dto.event;
import jakarta.validation.constraints.*; import lombok.*; import java.time.*;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventCreateRequest {
    @NotBlank @Size(max=40) private String boardCode; @NotBlank @Size(max=200) private String title; @NotBlank private String content;
    @NotNull private LocalDate startDate; @NotNull private LocalDate endDate; @Size(max=255) private String bannerUrl; private Boolean active;
}