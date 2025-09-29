package com.dodam.board.service;

import java.util.Date;
import java.util.Collections;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.dodam.board.service.BoardService;
import com.dodam.board.dto.notice.*;
import com.dodam.board.entity.BoardEntity;
import com.dodam.board.entity.NoticeEntity;
import com.dodam.board.error.NotFoundException;
import com.dodam.board.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NoticeService {
    private final NoticeRepository repo;
    private final BoardService boards;

    @Transactional
    public NoticeResponse create(NoticeCreateRequest req) {
        BoardEntity board = boards.getByCodeOrThrow(req.getBoardCode());
        NoticeEntity notice = NoticeEntity.builder()
                .boardCode(board)
                .title(req.getTitle())
                .content(req.getContent())
                .pinned(req.isPinned())
                .build();
        notice = repo.save(notice);
        return toDto(notice);
    }

    public Page<NoticeResponse> list(String boardCode, Pageable pageable) {
        return repo.findByBoardCode(boardCode, pageable).map(this::toDto);
    }

    public NoticeResponse get(Long id, boolean incrementViews) {
        NoticeEntity notice = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("공지 없음: id=" + id));
        if (incrementViews) {
            notice.setViews(notice.getViews() + 1);
            repo.save(notice);
        }
        return toDto(notice);
    }

    @Transactional
    public NoticeResponse update(Long id, NoticeUpdateRequest req) {
        NoticeEntity notice = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("공지 없음: id=" + id));
        
        if (req.getTitle() != null) notice.setTitle(req.getTitle());
        if (req.getContent() != null) notice.setContent(req.getContent());
        if (req.getPinned() != null) notice.setPinned(req.getPinned());
        if (req.getStatus() != null) {
            notice.setStatus(NoticeEntity.NoticeStatus.valueOf(req.getStatus()));
        }
        return toDto(notice);
    }

    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new NotFoundException("공지 없음: id=" + id);
        }
        repo.deleteById(id);
    }

    private NoticeResponse toDto(NoticeEntity notice) {
        return NoticeResponse.builder()
                .id(notice.getId())
                .boardCode(notice.getBoardCode().getCode()) // 수정된 부분
                .title(notice.getTitle())
                .content(notice.getContent())
                .pinned(notice.isPinned())
                .views(notice.getViews())
                .status(notice.getStatus().name())
                .createdAt(notice.getCreatedAt())
                .updatedAt(notice.getUpdatedAt())
                .build();
    }
}
