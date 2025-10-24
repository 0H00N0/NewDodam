package com.dodam.community.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.dodam.board.entity.BoardEntity;
import com.dodam.board.entity.CommentEntity;

public interface CommunityCommentRepository extends JpaRepository<CommentEntity, Long> {
    List<CommentEntity> findByBoardOrderByConumAsc(BoardEntity board);
    
    @Modifying
    @Transactional
    @Query("delete from CommentEntity c where c.board = :board")
    void deleteByBoard(@Param("board") BoardEntity board);
    
    // 게시글별 조회
    List<CommentEntity> findByBoard_BnumOrderByCdateAsc(Long bnum);

    // 대댓글 존재 여부(연관 통해 속성 타기: parent.conum)
    boolean existsByParent_Conum(Long conum);
}