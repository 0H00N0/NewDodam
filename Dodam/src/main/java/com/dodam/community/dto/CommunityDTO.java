// com.dodam.community.dto.CommunityDTO
package com.dodam.community.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CommunityDTO {
  @Getter @Setter public static class CreateReq {
    private Long mnum;
    private Long mtnum;
    private Long bcanum;
    private String mid;
    private String mnic;
    private String bsub;
    private String bcontent;
  }

  @Getter @Setter public static class UpdateReq {
    private Long bcanum;
    private Long bsnum;
    private String bsub;
    private String bcontent;
  }

  @Getter @Setter @Builder public static class Resp {
    private Long bnum;
    private Long mnum;
    private Long mtnum;
    private Long bcanum;
    private String bcaname;
    private Long bsnum;
    private String bsname;
    private String bsub;
    private String bcontent;
    private String mid;
    private String mnic;
    private LocalDateTime bdate;
    private LocalDateTime bedate;
    private Boolean mine;
  }

  @Getter @Setter public static class CommentCreateReq {
    private Long mnum;
    private String mid;
    private String mnic;
    private String ccontent;
    private Long parentConum;         // ✅ 대댓글 부모 ID (최상위는 null)
  }

  @Getter @Setter public static class CommentUpdateReq {
    private String ccontent;
  }

  @Getter @Setter @Builder
  public static class CommentResp {
    private Long conum;
    private Long bnum;
    private Long mnum;
    private String mid;
    private String mnic;
    private String ccontent;
    private LocalDateTime cdate;
    private LocalDateTime cedate;
    private Long parentConum;            // ✅ 부모 ID
    private Boolean mine; //댓글 표시

    @Builder.Default
    private List<CommentResp> children = new ArrayList<>(); // ✅ 가변 리스트 기본값
  }

  @Getter @Setter @Builder public static class PageResp<T> {
    private List<T> content;
    private long totalElements;
    private int totalPages;
    private int number;
    private int size;
  }
}
