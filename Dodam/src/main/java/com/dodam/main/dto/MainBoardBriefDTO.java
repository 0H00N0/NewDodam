// com/dodam/main/dto/MainBoardBriefDTO.java
package com.dodam.main.dto;
import lombok.AllArgsConstructor; import lombok.Getter;

@Getter @AllArgsConstructor
public class MainBoardBriefDTO {
    private Long   postId;      // bnum
    private String title;       // btitle
    private String author;      // mnic
    private String createdAt;   // ISO string
    private Long   bcnum;       // 카테고리 번호
    private String bcname;      // 카테고리명(참고용)
}
