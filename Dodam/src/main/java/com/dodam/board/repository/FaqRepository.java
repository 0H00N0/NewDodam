package com.dodam.board.repository;

import com.dodam.board.entity.FaqEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FaqRepository extends JpaRepository<FaqEntity, Long> {
    Page<FaqEntity> findByBoard_CodeAndEnabledTrueOrderBySortOrderAsc(String code, Pageable pageable);
    Page<FaqEntity> findByBoard_CodeAndCategoryAndEnabledTrueOrderBySortOrderAsc(String code, String category, Pageable pageable);
}