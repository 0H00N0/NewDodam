package com.dodam.board.repository;

import com.dodam.board.entity.BoardStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BoardStateRepository extends JpaRepository<BoardStateEntity, Long> {
}