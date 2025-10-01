// src/main/java/com/dodam/board/dto/EventUpdateRequest.java
package com.dodam.board.dto;

import com.dodam.board.entity.BoardCategoryEntity;
import com.dodam.board.entity.BoardEntity;
import com.dodam.board.entity.BoardStateEntity;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Function;

/**
 * 이벤트 게시글 업데이트 요청 DTO (부분 업데이트용)
 *
 * - 본 DTO는 PATCH 시나리오를 기본으로 합니다.
 *   => 요청 JSON에 들어온 필드만 엔티티에 반영합니다.
 * - BoardEntity의 스키마(bnum, mnum, mtnum, boardCategory(bcanum), boardState(bsnum),
 *   bsub, bcontent, bdate, bedate, mid, mnic)에 맞춰 필드를 제공합니다.
 * - bcanum/bsnum(코드)은 서비스에서 연관 엔티티로 해석 후 applyTo(...)에 전달하세요.
 *
 * 사용 흐름(권장):
 *   1) 컨트롤러에서 @Valid 로 본 DTO를 받음
 *   2) 서비스에서 req.applyTo(entity, categoryResolver, stateResolver) 호출
 *   3) 필요한 경우 bedate = now()로 갱신
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventUpdateRequest {

    // ====== 변경 가능 필드들(모두 선택값; null이면 스킵) ======

    /** 작성자 회원번호 -> BoardEntity.mnum (선택 업데이트) */
    @Min(value = 1, message = "mnum은 1 이상이어야 합니다.")
    private Long mnum;

    /** 상위/모듈 번호 -> BoardEntity.mtnum (선택 업데이트) */
    @Min(value = 1, message = "mtnum은 1 이상이어야 합니다.")
    private Long mtnum;

    /** 카테고리 코드(FK: bcanum) -> 서비스에서 BoardCategoryEntity 로 해석 */
    @Size(max = 40, message = "카테고리 코드는 최대 40자입니다.")
    private String bcanum;

    /** 상태 코드(FK: bsnum; 예: DRAFT/PUBLISHED/ARCHIVED) -> 서비스에서 BoardStateEntity 로 해석 */
    @Size(max = 40, message = "상태 코드는 최대 40자입니다.")
    private String bsnum;

    /** 제목 -> BoardEntity.bsub */
    @Size(max = 255, message = "제목은 최대 255자입니다.")
    private String bsub;

    /** 본문 -> BoardEntity.bcontent (DB 컬럼 4000) */
    @Size(max = 4000, message = "내용은 최대 4000자입니다.")
    private String bcontent;

    /** 작성자 로그인 ID -> BoardEntity.mid */
    @Size(max = 255, message = "작성자 아이디는 최대 255자입니다.")
    private String mid;

    /** 작성자 닉네임 -> BoardEntity.mnic */
    @Size(max = 255, message = "작성자 닉네임은 최대 255자입니다.")
    private String mnic;

    /** 작성일(선택) */
    private LocalDateTime bdate;

    /** 수정일(선택) — 보통 서비스에서 now()로 갱신 */
    private LocalDateTime bedate;

    // ====== 추가 유효성(필드가 들어왔을 때만 검사되도록 구현) ======

    @AssertTrue(message = "제목은 공백만으로는 수정할 수 없습니다.")
    public boolean isTitleNotBlankWhenPresent() {
        return bsub == null || !bsub.isBlank();
    }

    @AssertTrue(message = "내용은 공백만으로는 수정할 수 없습니다.")
    public boolean isContentNotBlankWhenPresent() {
        return bcontent == null || !bcontent.isBlank();
    }

    @AssertTrue(message = "작성자 아이디는 공백만으로는 수정할 수 없습니다.")
    public boolean isMidNotBlankWhenPresent() {
        return mid == null || !mid.isBlank();
    }

    @AssertTrue(message = "작성자 닉네임은 공백만으로는 수정할 수 없습니다.")
    public boolean isMnicNotBlankWhenPresent() {
        return mnic == null || !mnic.isBlank();
    }

    @AssertTrue(message = "수정일은 작성일 이후 또는 동일해야 합니다.")
    public boolean isEditDateValid() {
        if (bdate == null || bedate == null) return true; // 둘 다 있을 때만 비교
        return !bedate.isBefore(bdate);
    }

    // ====== 매핑/적용 유틸 ======

    /**
     * 전달된 값이 null이 아니면 엔티티에 적용합니다(부분 업데이트).
     * - bcanum/bsnum이 넘어오면 각각 리졸버로 엔티티를 구해 설정합니다.
     * - 리졸버가 null이거나 코드가 유효하지 않은 경우 예외를 던집니다.
     *
     * @param entity 대상 BoardEntity (영속 상태 권장)
     * @param categoryResolver bcanum -> BoardCategoryEntity
     * @param stateResolver    bsnum  -> BoardStateEntity
     */
    public void applyTo(
            BoardEntity entity,
            Function<String, BoardCategoryEntity> categoryResolver,
            Function<String, BoardStateEntity> stateResolver
    ) {
        if (entity == null) throw new IllegalArgumentException("entity는 null일 수 없습니다.");

        // 기본/숫자 필드
        if (mnum != null) entity.setMnum(mnum);
        if (mtnum != null) entity.setMtnum(mtnum);

        // 카테고리/상태(FK)
        if (bcanum != null) {
            if (categoryResolver == null)
                throw new IllegalArgumentException("categoryResolver가 필요합니다.");
            BoardCategoryEntity category = Optional.ofNullable(categoryResolver.apply(bcanum))
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리 코드: " + bcanum));
            entity.setBoardCategory(category);
        }
        if (bsnum != null) {
            if (stateResolver == null)
                throw new IllegalArgumentException("stateResolver가 필요합니다.");
            BoardStateEntity state = Optional.ofNullable(stateResolver.apply(bsnum))
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상태 코드: " + bsnum));
            entity.setBoardState(state);
        }

        // 문자열 필드(트림 적용)
        if (bsub != null) entity.setBsub(trimOrNullToNull(bsub));
        if (bcontent != null) entity.setBcontent(bcontent); // 본문은 공백 허용(위 @AssertTrue로 빈문자만 방지)
        if (mid != null) entity.setMid(trimOrNullToNull(mid));
        if (mnic != null) entity.setMnic(trimOrNullToNull(mnic));

        // 날짜
        if (bdate != null) entity.setBdate(bdate);
        if (bedate != null) entity.setBedate(bedate);

        // 최종 필수값 방어(엔티티의 not-null 컬럼이 null이면 예외)
        ensureNotNullColumns(entity);
    }

    /**
     * 서비스 계층에서 사용할 불변 커맨드로 변환.
     * - path variable로 bnum을 받는 패턴을 고려하여 파라미터로 bnum을 받습니다.
     */
    public UpdateCommand toCommand(Long bnum) {
        return new UpdateCommand(
                bnum,
                mnum, mtnum,
                bcanum, bsnum,
                bsub != null ? bsub.trim() : null,
                bcontent,
                mid != null ? mid.trim() : null,
                mnic != null ? mnic.trim() : null,
                bdate, bedate
        );
    }

    @Getter
    @RequiredArgsConstructor
    public static class UpdateCommand {
        private final Long bnum;
        private final Long mnum;
        private final Long mtnum;
        private final String bcanum;
        private final String bsnum;
        private final String bsub;
        private final String bcontent;
        private final String mid;
        private final String mnic;
        private final LocalDateTime bdate;
        private final LocalDateTime bedate;

        public boolean hasAnyChange() {
            return mnum != null || mtnum != null || bcanum != null || bsnum != null ||
                   bsub != null || bcontent != null || mid != null || mnic != null ||
                   bdate != null || bedate != null;
        }
    }

    // ====== 내부 헬퍼 ======

    /** 공백 또는 빈 문자열이면 null 로 치환 */
    private static String trimOrNullToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
        // 주의: mid/mnic/bsub가 null이 되면 not-null 제약을 위반할 수 있음 → ensureNotNullColumns에서 방어
    }

    /** 엔티티의 not-null 컬럼들에 대해 null 불가 방어 */
    private static void ensureNotNullColumns(BoardEntity e) {
        if (e.getMnum() == null)  throw new IllegalStateException("mnum은 null일 수 없습니다.");
        if (e.getMtnum() == null) throw new IllegalStateException("mtnum은 null일 수 없습니다.");
        if (e.getBoardCategory() == null) throw new IllegalStateException("boardCategory는 null일 수 없습니다.");
        if (e.getBoardState() == null)    throw new IllegalStateException("boardState는 null일 수 없습니다.");
        if (e.getMid() == null || e.getMid().isBlank())
            throw new IllegalStateException("mid는 null/공백일 수 없습니다.");
        if (e.getMnic() == null || e.getMnic().isBlank())
            throw new IllegalStateException("mnic은 null/공백일 수 없습니다.");
    }
}
