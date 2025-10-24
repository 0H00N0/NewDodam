// src/main/java/com/dodam/community/controller/CommunityCommentController.java
package com.dodam.community.controller;

import com.dodam.community.dto.CommunityDTO;
import com.dodam.community.service.CommunityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/board/community/{bnum}/comments")
public class CommunityCommentController {

    private final CommunityService boardService;

    @GetMapping
    public List<CommunityDTO.CommentResp> list(@PathVariable("bnum") Long bnum) {
        return boardService.listComments(bnum);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Long> add(@PathVariable("bnum") Long bnum,
                                    @Valid @RequestBody CommunityDTO.CommentCreateReq req,
                                    Authentication authentication) {
        if (authentication == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        req.setMid(authentication.getName());
        // 클라이언트가 보낸 mnic/mnum 무시
        req.setMnum(null);
        req.setMnic(null);
        Long cid = boardService.addComment(bnum, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(cid);
    }

    @PutMapping(value = "/{conum}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> update(@PathVariable("bnum") Long bnum,
                                       @PathVariable("conum") Long conum,
                                       @Valid @RequestBody CommunityDTO.CommentUpdateReq req,
                                       Authentication authentication) {
        if (authentication == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        boardService.updateComment(bnum, conum, req, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{conum}")
    public ResponseEntity<Void> delete(@PathVariable("bnum") Long bnum,
                                       @PathVariable("conum") Long conum,
                                       Authentication authentication) {
        if (authentication == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        boardService.deleteComment(bnum, conum, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
