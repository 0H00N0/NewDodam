// com.dodam.board.service.BoardService.java
package com.dodam.community.service;

import com.dodam.community.dto.CommunityDTO;

import java.util.List;

import org.springframework.data.domain.Page;

public interface CommunityService {
    Page<CommunityDTO.Resp> search(Long bcanum, Long bsnum, String q, int page, int size);
    CommunityDTO.Resp get(Long bnum);
    Long create(CommunityDTO.CreateReq req);
    void update(Long bnum, CommunityDTO.UpdateReq req, String actorMid);
    void delete(Long bnum, String actorMid);

    // comments
    List<CommunityDTO.CommentResp> listComments(Long bnum);
    Long addComment(Long bnum, CommunityDTO.CommentCreateReq req);
    void updateComment(Long bnum, Long conum, CommunityDTO.CommentUpdateReq req, String actorMid);
    void deleteComment(Long bnum, Long conum, String actorMid);
}
