package com.dodam.board.repository;

import com.dodam.board.entity.EventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;

public interface EventRepository extends JpaRepository<EventEntity, Long> {
    Page<EventEntity> findByCode(String code, Pageable pageable);
    Page<EventEntity> findByCodeAndActiveTrue(String code, Pageable pageable);
    Page<EventEntity> findByCodeAndStartDateLessThanEqualAndEndDateGreaterThanEqual(String code, LocalDate date1, LocalDate date2, Pageable pageable);
}