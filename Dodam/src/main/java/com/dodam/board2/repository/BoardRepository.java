package com.dodam.board2.repository;

import com.dodam.board.entity.BoardEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("boardRepository2")
public interface BoardRepository extends JpaRepository<BoardEntity, Long> {
    // 필요하다면 여기에 커스텀 쿼리 추가 가능
}
