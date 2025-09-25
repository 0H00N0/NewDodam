package com.dodam.board.dto;
import java.time.LocalDateTime;

import com.dodam.board.entity.BoardEntity;

import lombok.*;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BoardDto { 
	private String code; 
	private String name; 
	private String description; 
	private Long id;
    private String title;
    private String content;
    private String author;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static BoardDto from(BoardEntity e) {
        return BoardDto.builder()
                .code(e.getCode())
                .title(e.getBtitle())
                .content(e.getBcontent())
                .author(e.getMnic())
                .createdAt(e.getBdate())
                .updatedAt(e.getBedate())
                .id(e.getBnum())  
                .build();
    }
}