package com.dodam.board.service;

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
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class InquiryService {
    private final InquiryRepository repo;
    private final BoardService boards;
    private final BCryptPasswordEncoder enc = new BCryptPasswordEncoder();

    @Transactional
    public InquiryResponse create(InquiryCreateRequest req) {
        BoardEntity board = boards.getByCodeOrThrow(req.getCode());
        InquiryEntity inquiry = InquiryEntity.builder()
                .code(req.getCode() != null ? board : null)
                .title(req.getTitle())
                .content(req.getContent())
                .contactEmail(req.getContactEmail())
                .secret(req.isSecret())
                .status(InquiryEntity.InquiryStatus.OPEN)
                .build();

        if (req.isSecret() && req.getSecretPassword() != null && !req.getSecretPassword().isBlank()) {
            inquiry.setSecretPassword(enc.encode(req.getSecretPassword()));
        }
        
        inquiry = repo.save(inquiry);
        return toDto(inquiry, true);
    }

    public Page<InquiryResponse> list(String code, Pageable pageable) {
        return repo.findByBoard_Code(code, pageable)
                .map(inquiry -> toDto(inquiry, false));
    }

    public InquiryResponse get(Long id, String password) {
        InquiryEntity inquiry = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("문의 없음: id=" + id));
        
        boolean show = true;
        if (inquiry.isSecret()) {
            show = password != null && 
                   inquiry.getSecretPassword() != null && 
                   enc.matches(password, inquiry.getSecretPassword());
        }
        return toDto(inquiry, show);
    }

    @Transactional
    public InquiryResponse update(Long id, InquiryUpdateRequest req) {
        InquiryEntity inquiry = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("문의 없음: id=" + id));

        if (req.getTitle() != null) inquiry.setTitle(req.getTitle());
        if (req.getContent() != null) inquiry.setContent(req.getContent());
        if (req.getAnswerContent() != null) {
            inquiry.setAnswerContent(req.getAnswerContent());
            inquiry.setAnsweredAt(LocalDateTime.now());
            inquiry.setStatus(InquiryEntity.InquiryStatus.ANSWERED);
        }
        if (req.getStatus() != null) {
            inquiry.setStatus(InquiryEntity.InquiryStatus.valueOf(req.getStatus()));
        }
        
        return toDto(inquiry, true);
    }

    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new NotFoundException("문의 없음: id=" + id);
        }
        repo.deleteById(id);
    }

    private InquiryResponse toDto(InquiryEntity inquiry, boolean show) {
        String content = show ? inquiry.getContent() : 
                "[비밀글입니다. 올바른 비밀번호로 다시 요청하세요]";
        
        return InquiryResponse.builder()
                .id(inquiry.getId())
                .code(inquiry.getCode().getCode())
                .title(inquiry.getTitle())
                .content(content)
                .contactEmail(inquiry.getContactEmail())
                .secret(inquiry.isSecret())
                .status(inquiry.getStatus().name())
                .answerContent(inquiry.getAnswerContent())
                .answeredAt(inquiry.getAnsweredAt())
                .createdAt(inquiry.getCreatedAt())
                .updatedAt(inquiry.getUpdatedAt())
                .build();
    }
}
