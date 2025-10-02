package com.dodam.board.dto;

import java.time.LocalDateTime;
import com.dodam.board.entity.BoardEntity;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardDto {
    private Long bnum;
    private Long mnum;
    private Long mtnum;
    private String bcanum;
    private String bsnum;
    private String bsub;
    private String bcontent;
    private String mid;
    private String mnic;
    private LocalDateTime bdate;
    private LocalDateTime bedate;

    public static BoardDto from(BoardEntity entity) {
        return BoardDto.builder()
                .bnum(entity.getBnum())
                .mnum(entity.getMnum())
                .mtnum(entity.getMtnum())
                .bcanum(entity.getBoardCategory() != null ? entity.getBoardCategory().getBcanum() : null)
                .bsnum(entity.getBoardState() != null ? entity.getBoardState().getBsnum() : null)
                .bsub(entity.getBsub())
                .bcontent(entity.getBcontent())
                .mid(entity.getMid())
                .mnic(entity.getMnic())
                .bdate(entity.getBdate())
                .bedate(entity.getBedate())
                .build();        
    }
}
