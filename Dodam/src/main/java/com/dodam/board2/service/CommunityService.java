package com.dodam.board2.service;

import com.dodam.board.entity.CommunityEntity;
import com.dodam.board.repository.CommunityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommunityService {

    private final CommunityRepository communityRepository;

    // 글 저장
    public CommunityEntity savePost(CommunityEntity post) {
        return communityRepository.save(post);
    }
}