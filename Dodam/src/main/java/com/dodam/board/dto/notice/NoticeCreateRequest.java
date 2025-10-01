package com.dodam.board.dto.notice;
import jakarta.validation.constraints.*; import lombok.*;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NoticeCreateRequest {
    @NotBlank @Size(max=40) private String code;
    @NotBlank @Size(max=200) private String title;
    @NotBlank private String content;
    private boolean pinned;
}