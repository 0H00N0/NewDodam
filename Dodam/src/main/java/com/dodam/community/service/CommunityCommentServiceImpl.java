// src/main/java/com/dodam/community/service/CommunityCommentServiceImpl.java
package com.dodam.community.service;

import com.dodam.board.entity.CommentEntity;
import com.dodam.community.repository.CommunityCommentRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class CommunityCommentServiceImpl implements CommunityCommentService {

    private final CommunityCommentRepository commentRepo;

    @Override
    public Map<String, Object> updateContent(Long conum, String ccontent) {
        var c = commentRepo.findById(conum)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."));
        c.setCcontent(ccontent);  // ✅ 필드명과 일치
        return Map.of("conum", c.getConum(), "ccontent", c.getCcontent());
    }

    @Override
    public Map<String, Object> deleteOrSoftDelete(Long conum) {
        CommentEntity c = commentRepo.findById(conum)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."));

        boolean hasChildren = commentRepo.existsByParent_Conum(conum);
        if (hasChildren) {
            // 소프트 삭제: 내용 가림
            c.setCcontent("삭제된 댓글입니다.");
            return Map.of("conum", c.getConum(), "softDeleted", true);
        } else {
            commentRepo.delete(c);
            return Map.of("conum", conum, "deleted", true);
        }
    }
}
