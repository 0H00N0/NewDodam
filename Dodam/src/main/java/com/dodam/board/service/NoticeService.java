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

@Service @RequiredArgsConstructor public class NoticeService {
  private final NoticeRepository repo; private final BoardService boards;
  @Transactional public NoticeResponse create(NoticeCreateRequest req){ BoardEntity b=boards.getByCodeOrThrow(req.getBoardCode());
    NoticeEntity e=NoticeEntity.builder().boardCode(b).title(req.getTitle()).content(req.getContent()).pinned(req.isPinned()).build();
    e=repo.save(e); return toDto(e); }
  public Page<NoticeResponse> list(String boardCode, Pageable p){ return repo.findByBoardCode(boardCode,p).map(this::toDto); }
  public NoticeResponse get(Long id, boolean inc){ NoticeEntity e=repo.findById(id).orElseThrow(()->new NotFoundException("공지 없음: id="+id));
    if(inc){ e.setViews(e.getViews()+1); repo.save(e);} return toDto(e); }
  @Transactional public NoticeResponse update(Long id, NoticeUpdateRequest req){ NoticeEntity e=repo.findById(id).orElseThrow(()->new NotFoundException("공지 없음: id="+id));
    if(req.getTitle()!=null) e.setTitle(req.getTitle()); if(req.getContent()!=null) e.setContent(req.getContent()); if(req.getPinned()!=null) e.setPinned(req.getPinned());
    if(req.getStatus()!=null) e.setStatus(NoticeEntity.NoticeStatus.valueOf(req.getStatus())); return toDto(e); }
  public void delete(Long id){ if(!repo.existsById(id)) throw new NotFoundException("공지 없음: id="+id); repo.deleteById(id); }
  private NoticeResponse toDto(NoticeEntity e){ return NoticeResponse.builder().id(e.getId()).boardCode(e.getId().getClass()).title(e.getTitle()).content(e.getContent())
    .pinned(e.isPinned()).views(e.getViews()).status(e.getStatus().name()).createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt()).build(); }
}