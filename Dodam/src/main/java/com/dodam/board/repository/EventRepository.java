package com.dodam.board.repository;

import com.dodam.board.entity.EventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;

public interface EventRepository extends JpaRepository<EventEntity, Long> {
    Page<EventEntity> findByBoard_Code(String boardCode, Pageable pageable);
    Page<EventEntity> findByBoard_CodeAndActiveTrue(String boardCode, Pageable pageable);
    Page<EventEntity> findByBoard_CodeAndStartDateLessThanEqualAndEndDateGreaterThanEqual(String boardCode, LocalDate date1, LocalDate date2, Pageable pageable);
}