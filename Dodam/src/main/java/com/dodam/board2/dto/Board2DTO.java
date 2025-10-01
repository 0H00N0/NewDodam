package com.dodam.board2.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Board2DTO {
    private Long id;             // 글 번호
    private String title;        // 제목
    private String content;      // 내용
    private String writer;       // 작성자
    private LocalDateTime createdAt; // 작성일
}