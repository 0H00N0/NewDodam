package com.dodam.board2.controller;

import com.dodam.board.entity.CommunityEntity;
import com.dodam.board2.service.CommunityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;

    /** 글 등록 API */
    @PostMapping
    public ResponseEntity<CommunityEntity> createCommunityPost(@RequestBody CommunityEntity post) {
        CommunityEntity savedPost = communityService.savePost(post);
        return ResponseEntity.ok(savedPost);
    }
}