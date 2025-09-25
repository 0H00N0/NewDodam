package com.dodam.admin.dto;

import com.dodam.voc.entity.VocEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class VocAdminDto {

    /**
     * VOC 목록 조회를 위한 DTO
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VocListResponse {
        private Long vocId;
        private String title;
        private String authorName; // 작성자 이름
        private VocEntity.VocStatus status;
        private VocEntity.VocCategory category;
        private LocalDateTime createdAt;

        public static VocListResponse fromEntity(VocEntity voc) {
            return VocListResponse.builder()
                    .vocId(voc.getVocId())
                    .title(voc.getTitle())
                    .authorName(voc.getAuthor().getMname()) // MemberEntity에 getName()이 있다고 가정
                    .status(voc.getStatus())
                    .category(voc.getCategory())
                    .createdAt(voc.getCreatedAt())
                    .build();
        }
    }

    /**
     * VOC 상세 조회를 위한 DTO
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VocDetailResponse {
        private Long vocId;
        private String title;
        private String content;
        private String authorName;
        private String authorEmail; // 작성자 이메일
        private VocEntity.VocStatus status;
        private VocEntity.VocCategory category;
        private LocalDateTime createdAt;
        private String answer; // 답변 내용

        public static VocDetailResponse fromEntity(VocEntity voc) {
            return VocDetailResponse.builder()
                    .vocId(voc.getVocId())
                    .title(voc.getTitle())
                    .content(voc.getContent())
                    .authorName(voc.getAuthor().getMname())
                    .authorEmail(voc.getAuthor().getMemail()) // MemberEntity에 getEmail()이 있다고 가정
                    .status(voc.getStatus())
                    .category(voc.getCategory())
                    .createdAt(voc.getCreatedAt())
                    .answer(voc.getAnswer() != null ? voc.getAnswer().getContent() : null)
                    .build();
        }
    }

    /**
     * VOC 답변 및 상태 업데이트를 위한 DTO
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VocUpdateRequest {
        private String answerContent;
        private VocEntity.VocStatus status;
        private Long handlerMnum; // 담당자 Mnum
    }
}