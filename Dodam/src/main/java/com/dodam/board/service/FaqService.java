package com.dodam.board.service;
import com.dodam.board.*; 
import com.dodam.board.dto.faq.*;
import com.dodam.board.entity.BoardEntity;
import com.dodam.board.entity.FaqEntity;
import com.dodam.board.error.NotFoundException; 
import com.dodam.board.repository.FaqRepository;
import lombok.RequiredArgsConstructor; 
import org.springframework.data.domain.*; 
import org.springframework.stereotype.Service; 
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor public class FaqService {
  private final FaqRepository repo; private final BoardService boards;
  @Transactional public FaqResponse create(FaqCreateRequest req){ BoardEntity b=boards.getByCodeOrThrow(req.getBoardCode());
    FaqEntity e=FaqEntity.builder().board(b).category(req.getCategory()).question(req.getQuestion()).answer(req.getAnswer()).sortOrder(req.getSortOrder()==null?0:req.getSortOrder()).enabled(req.getEnabled()==null?true:req.getEnabled()).build();
    e=repo.save(e); return toDto(e); }
  public Page<FaqResponse> list(String boardCode, String category, Pageable p){ if(category!=null && !category.isBlank()) return repo.findByBoard_CodeAndCategoryAndEnabledTrueOrderBySortOrderAsc(boardCode, category, p).map(this::toDto);
    return repo.findByBoard_CodeAndEnabledTrueOrderBySortOrderAsc(boardCode, p).map(this::toDto); }
  public FaqResponse get(Long id){ FaqEntity e=repo.findById(id).orElseThrow(()->new NotFoundException("FAQ 없음: id="+id)); return toDto(e); }
  @Transactional public FaqResponse update(Long id, FaqUpdateRequest req){ FaqEntity e=repo.findById(id).orElseThrow(()->new NotFoundException("FAQ 없음: id="+id));
    if(req.getCategory()!=null) e.setCategory(req.getCategory()); if(req.getQuestion()!=null) e.setQuestion(req.getQuestion()); if(req.getAnswer()!=null) e.setAnswer(req.getAnswer());
    if(req.getSortOrder()!=null) e.setSortOrder(req.getSortOrder()); if(req.getEnabled()!=null) e.setEnabled(req.getEnabled()); return toDto(e); }
  public void delete(Long id){ if(!repo.existsById(id)) throw new NotFoundException("FAQ 없음: id="+id); repo.deleteById(id); }
  private FaqResponse toDto(FaqEntity e){ return FaqResponse.builder().id(e.getId()).boardCode(e.getBoard().getCode()).category(e.getCategory()).question(e.getQuestion()).answer(e.getAnswer())
    .sortOrder(e.getSortOrder()).enabled(e.isEnabled()).createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt()).build(); }
}