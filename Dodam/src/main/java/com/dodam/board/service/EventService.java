package com.dodam.board.service;

import com.dodam.board.dto.event.*;
import com.dodam.board.entity.BoardEntity;
import com.dodam.board.entity.EventEntity;
import com.dodam.board.error.NotFoundException;
import com.dodam.board.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class EventService {
    private final EventRepository repo;
    private final BoardService boards;

    @Transactional
    public EventResponse create(EventCreateRequest req) {
        if (req.getEndDate().isBefore(req.getStartDate())) {
            throw new IllegalArgumentException("endDate는 startDate보다 빠를 수 없습니다.");
        }
        BoardEntity board = boards.getByCodeOrThrow(req.getCode());
        EventEntity event = EventEntity.builder()
                .code(req.getCode() != null ? board : null)
                .title(req.getTitle())
                .content(req.getContent())
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .bannerUrl(req.getBannerUrl())
                .active(req.getActive() == null ? true : req.getActive())
                .build();
        event = repo.save(event);
        return toDto(event);
    }

    public Page<EventResponse> list(String code, Boolean activeOnly, LocalDate from, LocalDate to, Pageable pageable) {
        BoardEntity board = boards.getByCodeOrThrow(code);
        if (Boolean.TRUE.equals(activeOnly)) {
            return repo.findByCodeAndActiveTrue(code, pageable).map(this::toDto);
        }
        if (from != null && to != null) {
            return repo.findByCodeAndStartDateLessThanEqualAndEndDateGreaterThanEqual(code, to, from, pageable).map(this::toDto);
        }
        return repo.findByCode(code, pageable).map(this::toDto);
    }

    public EventResponse get(Long id, boolean inc) {
        EventEntity event = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("이벤트 없음: id=" + id));
        if (inc) {
            event.setViews(event.getViews() + 1);
            repo.save(event);
        }
        return toDto(event);
    }

    @Transactional
    public EventResponse update(Long id, EventUpdateRequest req) {
        EventEntity event = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("이벤트 없음: id=" + id));
        
        if (req.getTitle() != null) event.setTitle(req.getTitle());
        if (req.getContent() != null) event.setContent(req.getContent());
        if (req.getStartDate() != null) event.setStartDate(req.getStartDate());
        if (req.getEndDate() != null) event.setEndDate(req.getEndDate());
        
        if (event.getEndDate().isBefore(event.getStartDate())) {
            throw new IllegalArgumentException("endDate는 startDate보다 빠를 수 없습니다.");
        }
        
        if (req.getBannerUrl() != null) event.setBannerUrl(req.getBannerUrl());
        if (req.getActive() != null) event.setActive(req.getActive());
        
        return toDto(event);
    }

    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new NotFoundException("이벤트 없음: id=" + id);
        }
        repo.deleteById(id);
    }

    private EventResponse toDto(EventEntity event) {
        return EventResponse.builder()
                .id(event.getId())
                .code(event.getCode().getCode())  // BoardEntity에서 code 값을 가져옴
                .title(event.getTitle())
                .content(event.getContent())
                .startDate(event.getStartDate())
                .endDate(event.getEndDate())
                .bannerUrl(event.getBannerUrl())
                .active(event.isActive())
                .views(event.getViews())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }
}
