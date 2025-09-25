package com.dodam.admin.dto;

import java.time.LocalDateTime;

import com.dodam.board.entity.BoardCategoryEntity;
import com.dodam.board.entity.BoardEntity;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class BoardManagementDTO {

    /**
     * 게시판 카테고리 생성을 위한 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class CreateBoardCategoryRequest {
        private String categoryName;

        // DTO를 Entity로 변환하는 메서드
        public BoardCategoryEntity toEntity() {
            BoardCategoryEntity entity = new BoardCategoryEntity();
            entity.setBcname(this.categoryName);
            return entity;
        }
    }

    /**
     * 게시판 카테고리 정보 응답 DTO
     */
    @Getter
    @Builder
    public static class BoardCategoryResponse {
        private Long id;
        private String name;

        // Entity를 DTO로 변환하는 정적 팩토리 메서드
        public static BoardCategoryResponse fromEntity(BoardCategoryEntity entity) {
            return BoardCategoryResponse.builder()
                    .id(entity.getBcnum())
                    .name(entity.getBcname())
                    .build();
        }
    }
    /**
     * 게시글 정보 응답 DTO
     */
    @Getter
    @Builder
    public static class PostResponse {
        private Long id; // bnum
        private String title; // btitle
        private String authorId; // mid
        private String authorNickname; // mnic
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")   // 추가
        private LocalDateTime createdAt; // bdate

        public static PostResponse fromEntity(BoardEntity entity) {
            return PostResponse.builder()
                    .id(entity.getBnum())
                    .title(entity.getBtitle())
                    .authorId(entity.getMid())
                    .authorNickname(entity.getMnic())
                    .createdAt(entity.getBdate())
                    .build();
        }
    }
    /**
     * 게시글 상세 정보 응답 DTO
     */
    @Getter
    @Builder
    public static class PostDetailResponse {
        private Long id;
        private String title;
        private String content; // 내용 추가
        private String authorId;
        private String authorNickname;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")   // 추가
        private LocalDateTime createdAt;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")   // 추가
        private LocalDateTime updatedAt; // 수정일 추가

        public static PostDetailResponse fromEntity(BoardEntity entity) {
            return PostDetailResponse.builder()
                    .id(entity.getBnum())
                    .title(entity.getBtitle())
                    .content(entity.getBcontent())
                    .authorId(entity.getMid())
                    .authorNickname(entity.getMnic())
                    .createdAt(entity.getBdate())
                    .updatedAt(entity.getBedate())
                    .build();
        }
    }

    /**
     * 게시글 생성을 위한 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class CreatePostRequest {
        private Long categoryId; // 어느 게시판에 쓸지
        private String title;
        private String content;
    }
}