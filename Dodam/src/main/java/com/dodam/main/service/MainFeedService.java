package com.dodam.main.service;

import com.dodam.main.dto.MainBoardBriefDTO;
import com.dodam.main.dto.MainReviewBriefDTO;
import com.dodam.main.repository.MainFeedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MainFeedService {

    private final MainFeedRepository repo;

    // ===== 리뷰: 그대로 유지 =====
    public List<MainReviewBriefDTO> getLatestReviews(int limit) {
        return repo.findLatestReviews(limit).stream()
            .map(r -> new MainReviewBriefDTO(
                toLong(r[0]),        // REVID
                (String) r[1],       // TITLE
                toInt(r[2]),         // SCORE
                (String) r[3],       // CREATED_AT
                toLong(r[4]),        // PROID
                (String) r[5],       // PRONAME
                (String) r[6]        // IMAGE_URL
            ))
            .collect(Collectors.toList());
    }

    // ===== 게시글: BSUB를 TITLE로 받아 매핑 (쿼리 순서에 맞춤)
    public List<MainBoardBriefDTO> getLatestBoards(Long bcanum, int limit) {
        return repo.findLatestBoardsByBcanum(bcanum, limit).stream()
            .map(r -> new MainBoardBriefDTO(
                toLong(r[0]), (String) r[1], (String) r[2], (String) r[3],
                toLong(r[4]), (String) r[5]
            ))
            .collect(Collectors.toList());
    }

    public List<MainBoardBriefDTO> getPopularBoards(Long bcanum, int limit) {
        return repo.findPopularBoardsByBcanum(bcanum, limit).stream()
            .map(r -> new MainBoardBriefDTO(
                toLong(r[0]), (String) r[1], (String) r[2], (String) r[3],
                toLong(r[4]), (String) r[5]
            ))
            .collect(Collectors.toList());
    }

    // ===== helpers =====
    private Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Long l) return l;
        if (o instanceof Integer i) return i.longValue();
        if (o instanceof BigInteger bi) return bi.longValue();
        if (o instanceof BigDecimal bd) return bd.longValue();
        return Long.parseLong(o.toString());
    }
    private Integer toInt(Object o) {
        if (o == null) return null;
        if (o instanceof Integer i) return i;
        if (o instanceof Long l) return l.intValue();
        if (o instanceof BigInteger bi) return bi.intValue();
        if (o instanceof BigDecimal bd) return bd.intValue();
        return Integer.parseInt(o.toString());
    }
}
