// src/main/java/com/dodam/board/dto/EventResponse.java
package com.dodam.board.dto.event;

import com.dodam.board.entity.BoardEntity;
import com.dodam.board.entity.BoardCategoryEntity;
import com.dodam.board.entity.BoardStateEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.annotation.Nullable;
import lombok.*;

import java.time.LocalDateTime;
import java.util.function.Function;

/**
 * 이벤트 게시글 응답 DTO
 *
 * - BoardEntity 스키마(bnum, mnum, mtnum, bcanum, bsnum, bsub, bcontent, bdate, bedate, mid, mnic)에
 *   맞춘 읽기 전용 응답 모델입니다.
 * - 카테고리/상태는 연관 엔티티이므로 기본적으로 "코드"만 포함(bcanum/bsnum).
 *   UI용 표시명이 필요하면 categoryName/stateName을 선택적으로 채우세요.
 * - 목록 응답에서 본문을 줄이고 싶으면 contentPreview를 사용하세요.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventResponse {

    // ====== 식별/기본 메타 ======
    /** 게시글 번호 */
    private Long bnum;

    /** 작성자 회원 번호 */
    private Long mnum;

    /** 상위/모듈 번호(도메인 정의에 따름) */
    private Long mtnum;

    // ====== 카테고리/상태 (코드 중심) ======
    /** 카테고리 코드 (FK: bcanum) */
    private String bcanum;

    /** 상태 코드 (FK: bsnum; 예: DRAFT/PUBLISHED/ARCHIVED 등) */
    private String bsnum;

    /** (선택) 카테고리 표시명 */
    private String categoryName;

    /** (선택) 상태 표시명 */
    private String stateName;

    // ====== 콘텐츠 ======
    /** 제목 */
    private String bsub;

    /** 본문(상세 응답에서 주로 사용) */
    private String bcontent;

    /** (선택) 본문 미리보기 (목록 응답 등에서 사용) */
    private String contentPreview;

    // ====== 작성자 표기 ======
    /** 작성자 로그인 ID */
    private String mid;

    /** 작성자 닉네임 */
    private String mnic;

    // ====== 날짜 ======
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime bdate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime bedate;

    // ------------------------------------------------------------
    // 정적 팩토리 메서드(매핑 유틸)
    // ------------------------------------------------------------

    /**
     * 엔티티에서 코드만 매핑하는 기본 변환(표시명 없음).
     * 주의: boardCategory/boardState 접근 시 LAZY 초기화가 발생할 수 있습니다.
     */
    public static EventResponse from(BoardEntity e) {
        if (e == null) return null;

        String catCode = null;
        BoardCategoryEntity cat = e.getBoardCategory();
        if (cat != null) {
            // LAZY라면 여기서 초기화될 수 있음
            catCode = cat.getBcanum();
        }

        String stateCode = null;
        BoardStateEntity st = e.getBoardState();
        if (st != null) {
            stateCode = st.getBsnum();
        }

        return EventResponse.builder()
                .bnum(e.getBnum())
                .mnum(e.getMnum())
                .mtnum(e.getMtnum())
                .bcanum(catCode)
                .bsnum(stateCode)
                .bsub(e.getBsub())
                .bcontent(e.getBcontent())
                .mid(e.getMid())
                .mnic(e.getMnic())
                .bdate(e.getBdate())
                .bedate(e.getBedate())
                .build();
    }

    /**
     * 엔티티에서 코드 + 표시명을 함께 매핑하는 변환(표시명은 문자열 인자로 주입).
     * - 카테고리/상태 표시명 계산을 이미 해두었을 때 사용하세요.
     */
    public static EventResponse from(BoardEntity e,
                                     @Nullable String categoryName,
                                     @Nullable String stateName) {
        EventResponse resp = from(e);
        if (resp == null) return null;
        resp.setCategoryName(trimOrNull(categoryName));
        resp.setStateName(trimOrNull(stateName));
        return resp;
    }

    /**
     * 엔티티에서 코드 + 표시명을 함께 매핑(표시명은 코드 기반 리졸버로 계산).
     * - categoryNameResolver: bcanum -> label
     * - stateNameResolver   : bsnum  -> label
     */
    public static EventResponse from(BoardEntity e,
                                     @Nullable Function<String, String> categoryNameResolver,
                                     @Nullable Function<String, String> stateNameResolver) {
        EventResponse resp = from(e);
        if (resp == null) return null;

        if (categoryNameResolver != null && resp.getBcanum() != null) {
            resp.setCategoryName(trimOrNull(categoryNameResolver.apply(resp.getBcanum())));
        }
        if (stateNameResolver != null && resp.getBsnum() != null) {
            resp.setStateName(trimOrNull(stateNameResolver.apply(resp.getBsnum())));
        }
        return resp;
    }

    /**
     * 목록 응답 등에서 본문 대신 미리보기만 포함하고 싶을 때 사용하는 변환.
     *
     * @param e           원본 엔티티
     * @param previewLen  미리보기 최대 길이(문자 수)
     * @param stripHtml   true면 아주 단순하게 태그를 제거하여 추출(정교한 Sanitize는 별도 라이브러리 권장)
     */
    public static EventResponse summary(BoardEntity e, int previewLen, boolean stripHtml) {
        EventResponse resp = from(e);
        if (resp == null) return null;
        resp.setContentPreview(preview(e.getBcontent(), previewLen, stripHtml));
        resp.setBcontent(null); // 목록에는 본문 전체 제외
        return resp;
    }

    // ------------------------------------------------------------
    // 헬퍼
    // ------------------------------------------------------------

    private static String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /**
     * 매우 단순한 프리뷰/스트립 유틸.
     * - stripHtml=true면 정규식으로 태그를 제거(치환 정확도는 제한적).
     * - 프로덕션에서는 Jsoup 등 HTML 파서를 권장.
     */
    private static String preview(String content, int limit, boolean stripHtml) {
        if (content == null) return null;
        String base = stripHtml ? content.replaceAll("<[^>]*>", " ") : content;
        base = base.replaceAll("\\s+", " ").trim();
        if (limit <= 0 || base.length() <= limit) return base;
        return base.substring(0, limit) + "…";
    }
}
