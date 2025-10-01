// src/main/java/com/dodam/board/dto/EventCreateRequest.java
package com.dodam.board.dto.event;

import com.dodam.board.entity.BoardCategoryEntity;
import com.dodam.board.entity.BoardEntity;
import com.dodam.board.entity.BoardStateEntity;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.function.Function;

/**
 * 이벤트 게시글 생성 요청 DTO
 *
 * BoardEntity 스키마와 1:1로 매핑 가능한 필드만 담았습니다.
 * - 카테고리/상태는 FK 이므로 코드(bcanum/bsnum)를 받고,
 *   서비스에서 BoardCategoryEntity/BoardStateEntity로 변환하여 toEntity(...)에 주입합니다.
 *
 * 사용 흐름(권장):
 *  1) 컨트롤러에서 @Valid 로 본 DTO를 받음
 *  2) 서비스에서 bcanum/bsnum을 기준으로 참조 엔티티 조회
 *  3) toEntity(category, state) 호출로 BoardEntity 생성
 *  4) 저장
 *
 * 날짜 필드:
 *  - bdate(작성일): null이면 서비스에서 now()로 설정 권장
 *  - bedate(수정일): 신규 생성시 보통 null 유지
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventCreateRequest {

    // ====== 필수 식별/메타 ======
    /** 작성자 회원 번호 -> BoardEntity.mnum */
    @NotNull(message = "작성자 회원번호(mnum)는 필수입니다.")
    private Long mnum;

    /** 상위/메뉴/모듈 번호 등 도메인 정의에 따름 -> BoardEntity.mtnum */
    @NotNull(message = "mtnum은 필수입니다.")
    private Long mtnum;

    /** 보드 카테고리 코드(FK) -> boardCategory(bcanum) */
    @NotBlank(message = "카테고리 코드(bcanum)는 필수입니다.")
    @Size(max = 40, message = "카테고리 코드는 최대 40자까지 가능합니다.")
    private String bcanum;

    /** 보드 상태 코드(FK) -> boardState(bsnum) (예: DRAFT/PUBLISHED/ARCHIVED 등) */
    @NotBlank(message = "상태 코드(bsnum)는 필수입니다.")
    @Size(max = 40, message = "상태 코드는 최대 40자까지 가능합니다.")
    private String bsnum;

    // ====== 콘텐츠 ======
    /** 제목 -> BoardEntity.bsub */
    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 255, message = "제목은 최대 255자까지 가능합니다.")
    private String bsub;

    /** 내용 -> BoardEntity.bcontent (DB 컬럼 4000) */
    @NotBlank(message = "내용은 필수입니다.")
    @Size(max = 4000, message = "내용은 최대 4000자까지 가능합니다.")
    private String bcontent;

    // ====== 작성자 표기 ======
    /** 작성자 로그인 ID -> BoardEntity.mid */
    @NotBlank(message = "작성자 아이디(mid)는 필수입니다.")
    @Size(max = 255, message = "작성자 아이디는 최대 255자까지 가능합니다.")
    private String mid;

    /** 작성자 닉네임 -> BoardEntity.mnic */
    @NotBlank(message = "작성자 닉네임(mnic)은 필수입니다.")
    @Size(max = 255, message = "작성자 닉네임은 최대 255자까지 가능합니다.")
    private String mnic;

    // ====== 날짜 ======
    /** 작성일(선택). null이면 서비스에서 now()로 설정 권장 */
    private LocalDateTime bdate;

    /** 수정일(선택). 신규 생성 시 보통 null */
    private LocalDateTime bedate;

    // ====== 추가적인 일관성 검증 ======
    @AssertTrue(message = "수정일은 작성일 이후 또는 동일해야 합니다.")
    public boolean isEditDateValid() {
        if (bdate == null || bedate == null) return true; // 선택값이면 스킵
        return !bedate.isBefore(bdate);
    }

    // ====== 매핑 유틸 ======

    /**
     * 서비스 계층에서 조회한 참조 엔티티로 즉시 BoardEntity를 생성합니다.
     *
     * @param category BoardCategoryEntity (필수)
     * @param state    BoardStateEntity (필수)
     * @return 새 BoardEntity (미저장 상태)
     */
    public BoardEntity toEntity(BoardCategoryEntity category, BoardStateEntity state) {
        if (category == null) {
            throw new IllegalArgumentException("boardCategory(BoardCategoryEntity)는 null일 수 없습니다.");
        }
        if (state == null) {
            throw new IllegalArgumentException("boardState(BoardStateEntity)는 null일 수 없습니다.");
        }

        // 작성일 기본값(now) 설정은 서비스에서 통일성 있게 처리 권장
        LocalDateTime createdAt = (this.bdate != null) ? this.bdate : LocalDateTime.now();

        return BoardEntity.builder()
                .mnum(this.mnum)
                .mtnum(this.mtnum)
                .boardCategory(category)   // FK: bcanum
                .boardState(state)         // FK: bsnum
                .bsub(this.bsub != null ? this.bsub.trim() : null)
                .bcontent(this.bcontent)
                .bdate(createdAt)
                .bedate(this.bedate)       // 보통 null로 생성
                .mid(this.mid != null ? this.mid.trim() : null)
                .mnic(this.mnic != null ? this.mnic.trim() : null)
                .build();
    }

    /**
     * 카테고리/상태 엔티티를 코드로부터 조회할 수 있는 리졸버(Function)를 받아 BoardEntity로 변환합니다.
     * - 레이어드 아키텍처에서 서비스가 finder를 주입해 사용할 때 편리합니다.
     *
     * @param categoryResolver  (bcanum -> BoardCategoryEntity)
     * @param stateResolver     (bsnum  -> BoardStateEntity)
     */
    public BoardEntity toEntity(Function<String, BoardCategoryEntity> categoryResolver,
                               Function<String, BoardStateEntity> stateResolver) {
        if (categoryResolver == null || stateResolver == null) {
            throw new IllegalArgumentException("Resolver는 null일 수 없습니다.");
        }
        BoardCategoryEntity category = categoryResolver.apply(this.bcanum);
        BoardStateEntity state = stateResolver.apply(this.bsnum);
        return toEntity(category, state);
    }

    // ====== 서비스 전달용 불변 커맨드 (선택) ======

    /**
     * 저장 서비스에서 사용하기 좋은 불변 커맨드로 변환합니다.
     * - 트랜잭션/조회 로직은 서비스에서 처리하고, 컨트롤러-서비스 경계를 명확히 합니다.
     */
    public CreateCommand toCommand() {
        return new CreateCommand(
                mnum, mtnum, bcanum, bsnum,
                bsub != null ? bsub.trim() : null,
                bcontent,
                mid != null ? mid.trim() : null,
                mnic != null ? mnic.trim() : null,
                bdate, bedate
        );
    }

    @Getter
    @RequiredArgsConstructor
    public static class CreateCommand {
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
    }
}
