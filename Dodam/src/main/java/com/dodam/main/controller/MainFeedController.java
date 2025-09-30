package com.dodam.main.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dodam.main.dto.MainBoardBriefDTO;
import com.dodam.main.dto.MainReviewBriefDTO;
import com.dodam.main.service.MainFeedService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/main")
public class MainFeedController {

    private final MainFeedService svc;

    // ====== 기존 범용 ======
    @GetMapping("/reviews")
    public List<MainReviewBriefDTO> latestReviews(
            @RequestParam(name = "limit", defaultValue = "3") int limit) {
        return svc.getLatestReviews(limit);
    }

    @GetMapping("/boards/latest")
    public List<MainBoardBriefDTO> latestBoards(
            @RequestParam(name = "bcnum") Long bcnum,
            @RequestParam(name = "limit", defaultValue = "3") int limit) {
        return svc.getLatestBoards(bcnum, limit);
    }

    @GetMapping("/boards/popular")
    public List<MainBoardBriefDTO> popularBoards(
            @RequestParam(name = "bcnum") Long bcnum,
            @RequestParam(name = "limit", defaultValue = "3") int limit) {
        return svc.getPopularBoards(bcnum, limit);
    }

    // ====== 편의용(카테고리 고정) ======
    private static final long BC_NOTICE = 1L;
    private static final long BC_COMMUNITY = 21L;

    /** 최신 공지 3개(기본) */
    @GetMapping("/notice/latest")
    public List<MainBoardBriefDTO> latestNotices(
            @RequestParam(name = "limit", defaultValue = "3") int limit) {
        return svc.getLatestBoards(BC_NOTICE, limit);
    }

    /** 커뮤니티 최신 3개(기본) */
    @GetMapping("/community/latest")
    public List<MainBoardBriefDTO> latestCommunity(
            @RequestParam(name = "limit", defaultValue = "3") int limit) {
        return svc.getLatestBoards(BC_COMMUNITY, limit);
    }

    /** 커뮤니티 인기 3개(기본, 임시 최신순) */
    @GetMapping("/community/popular")
    public List<MainBoardBriefDTO> popularCommunity(
            @RequestParam(name = "limit", defaultValue = "3") int limit) {
        return svc.getPopularBoards(BC_COMMUNITY, limit);
    }
}
