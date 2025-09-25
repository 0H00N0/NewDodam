package com.dodam.board.service;
import com.dodam.board.*; 
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
@Service @RequiredArgsConstructor public class EventService {
  private final EventRepository repo; private final BoardService boards;
  @Transactional public EventResponse create(EventCreateRequest req){ if(req.getEndDate().isBefore(req.getStartDate())) throw new IllegalArgumentException("endDate는 startDate보다 빠를 수 없습니다.");
    BoardEntity b=boards.getByCodeOrThrow(req.getBoardCode()); EventEntity e=EventEntity.builder().board(b).title(req.getTitle()).content(req.getContent())
      .startDate(req.getStartDate()).endDate(req.getEndDate()).bannerUrl(req.getBannerUrl()).active(req.getActive()==null?true:req.getActive()).build();
    e=repo.save(e); return toDto(e); }
  public Page<EventResponse> list(String boardCode, Boolean activeOnly, LocalDate from, LocalDate to, Pageable p){
    if(Boolean.TRUE.equals(activeOnly)) return repo.findByBoard_CodeAndActiveTrue(boardCode, p).map(this::toDto);
    if(from!=null && to!=null) return repo.findByBoard_CodeAndStartDateLessThanEqualAndEndDateGreaterThanEqual(boardCode, to, from, p).map(this::toDto);
    return repo.findByBoard_Code(boardCode, p).map(this::toDto);
  }
  public EventResponse get(Long id, boolean inc){ EventEntity e=repo.findById(id).orElseThrow(()->new NotFoundException("이벤트 없음: id="+id));
    if(inc){ e.setViews(e.getViews()+1); repo.save(e);} return toDto(e); }
  @Transactional public EventResponse update(Long id, EventUpdateRequest req){ EventEntity e=repo.findById(id).orElseThrow(()->new NotFoundException("이벤트 없음: id="+id));
    if(req.getTitle()!=null) e.setTitle(req.getTitle()); if(req.getContent()!=null) e.setContent(req.getContent());
    if(req.getStartDate()!=null) e.setStartDate(req.getStartDate()); if(req.getEndDate()!=null) e.setEndDate(req.getEndDate());
    if(e.getEndDate().isBefore(e.getStartDate())) throw new IllegalArgumentException("endDate는 startDate보다 빠를 수 없습니다.");
    if(req.getBannerUrl()!=null) e.setBannerUrl(req.getBannerUrl()); if(req.getActive()!=null) e.setActive(req.getActive()); return toDto(e); }
  public void delete(Long id){ if(!repo.existsById(id)) throw new NotFoundException("이벤트 없음: id="+id); repo.deleteById(id); }
  private EventResponse toDto(EventEntity e){ return EventResponse.builder().id(e.getId()).boardCode(e.getBoard().getCode()).title(e.getTitle()).content(e.getContent())
    .startDate(e.getStartDate()).endDate(e.getEndDate()).bannerUrl(e.getBannerUrl()).active(e.isActive()).views(e.getViews()).createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt()).build(); }
}