package com.dodam.board.repository;

import com.dodam.board.entity.FaqEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FaqRepository extends JpaRepository<FaqEntity, Long> {
    Page<FaqEntity> findByBoard_CodeAndEnabledTrueOrderBySortOrderAsc(String boardCode, Pageable pageable);
    Page<FaqEntity> findByBoard_CodeAndCategoryAndEnabledTrueOrderBySortOrderAsc(String boardCode, String category, Pageable pageable);
}