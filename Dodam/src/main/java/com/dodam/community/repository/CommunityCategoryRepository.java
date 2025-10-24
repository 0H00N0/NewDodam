package com.dodam.community.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dodam.board.entity.BoardCategoryEntity;

public interface CommunityCategoryRepository extends JpaRepository<BoardCategoryEntity, Long> {

}
