package com.dodam.community.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dodam.board.entity.BoardStateEntity;

public interface CommunityStateRepository extends JpaRepository<BoardStateEntity, Long>{

}
