package com.dodam.board.repository;

import com.dodam.board.entity.CommunityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommunityRepository extends JpaRepository<CommunityEntity, Long> {
    // 기본적인 CRUD 메서드는 JpaRepository가 자동 제공해줘
}