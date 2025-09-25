package com.dodam.board.repository;

import com.dodam.board.entity.BoardCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 게시판 카테고리(BoardCategoryEntity)를 위한 Spring Data JPA 리포지토리입니다.
 * 이 인터페이스를 통해 데이터베이스 CRUD 작업을 수행합니다.
 */
@Repository
public interface BoardCategoryRepository extends JpaRepository<BoardCategoryEntity, Long> {
    // JpaRepository가 기본적인 save(), findById(), findAll(), deleteById() 등의 메서드를 제공합니다.
    // 추가적인 커스텀 쿼리가 필요할 경우 여기에 메서드를 정의할 수 있습니다.
}