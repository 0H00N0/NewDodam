package com.dodam.board.repository;

import com.dodam.board.entity.BoardEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BoardRepository extends JpaRepository<BoardEntity, Long> {
    boolean existsByCode(String code);
}