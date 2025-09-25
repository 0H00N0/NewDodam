package com.dodam.board.service;
import com.dodam.board.*; 
import com.dodam.board.dto.inquiry.*;
import com.dodam.board.entity.BoardEntity;
import com.dodam.board.entity.InquiryEntity;
import com.dodam.board.error.NotFoundException; 
import com.dodam.board.repository.InquiryRepository;
import lombok.RequiredArgsConstructor; 
import org.springframework.data.domain.*; 
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service; 
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor public class InquiryService {
  private final InquiryRepository repo; private final BoardService boards; private final BCryptPasswordEncoder enc=new BCryptPasswordEncoder();
  @Transactional public InquiryResponse create(InquiryCreateRequest req){ BoardEntity b=boards.getByCodeOrThrow(req.getBoardCode());
    InquiryEntity e=InquiryEntity.builder().board(b).title(req.getTitle()).content(req.getContent()).contactEmail(req.getContactEmail()).secret(req.isSecret()).status(InquiryEntity.InquiryStatus.OPEN).build();
    if(req.isSecret() && req.getSecretPassword()!=null && !req.getSecretPassword().isBlank()) e.setSecretPassword(enc.encode(req.getSecretPassword()));
    e=repo.save(e); return toDto(e,true); }
  public Page<InquiryResponse> list(String boardCode, Pageable p){ return repo.findByBoard_Code(boardCode,p).map(e->toDto(e,false)); }
  public InquiryResponse get(Long id, String password){ InquiryEntity e=repo.findById(id).orElseThrow(()->new NotFoundException("문의 없음: id="+id));
    boolean show=true; if(e.isSecret()){ show= password!=null && e.getSecretPassword()!=null && enc.matches(password, e.getSecretPassword()); }
    return toDto(e, show); }
  @Transactional public InquiryResponse update(Long id, InquiryUpdateRequest req){ InquiryEntity e=repo.findById(id).orElseThrow(()->new NotFoundException("문의 없음: id="+id));
    if(req.getTitle()!=null) e.setTitle(req.getTitle()); if(req.getContent()!=null) e.setContent(req.getContent());
    if(req.getAnswerContent()!=null){ e.setAnswerContent(req.getAnswerContent()); e.setAnsweredAt(java.time.LocalDateTime.now()); e.setStatus(InquiryEntity.InquiryStatus.ANSWERED); }
    if(req.getStatus()!=null) e.setStatus(InquiryEntity.InquiryStatus.valueOf(req.getStatus())); return toDto(e,true); }
  public void delete(Long id){ if(!repo.existsById(id)) throw new NotFoundException("문의 없음: id="+id); repo.deleteById(id); }
  private InquiryResponse toDto(InquiryEntity e, boolean show){ String c= show? e.getContent() : "[비밀글입니다. 올바른 비밀번호로 다시 요청하세요]";
    return InquiryResponse.builder().id(e.getId()).boardCode(e.getBoard().getCode()).title(e.getTitle()).content(c).contactEmail(e.getContactEmail()).secret(e.isSecret())
      .status(e.getStatus().name()).answerContent(e.getAnswerContent()).answeredAt(e.getAnsweredAt()).createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt()).build(); }
}