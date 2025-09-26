package com.dodam.board.repository;

import com.dodam.board.entity.BoardEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BoardRepository extends JpaRepository<BoardEntity, Long> {
    boolean existsByCode(String code);
    
 // ▼▼▼ 이 코드를 추가해주세요 ▼▼▼
    /**
     * 카테고리 번호(bcnum)로 모든 게시글을 조회하고,
     * 게시글 번호(bnum)를 기준으로 내림차순 정렬합니다.
     * BoardEntity -> boardCategory -> bcnum 경로로 탐색합니다.
     */
    List<BoardEntity> findByBoardCategory_BcnumOrderByBnumDesc(Long bcnum);
}