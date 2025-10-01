package com.dodam.board.service;

import com.dodam.board.dto.faq.*;
import com.dodam.board.entity.BoardEntity;
import com.dodam.board.entity.FaqEntity;
import com.dodam.board.error.NotFoundException;
import com.dodam.board.repository.FaqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FaqService {
    private final FaqRepository repo;
    private final BoardService boards;

    @Transactional
    public FaqResponse create(FaqCreateRequest req) {
        BoardEntity board = boards.getByCodeOrThrow(req.getCode());
        FaqEntity faq = FaqEntity.builder()
                .code(req.getCode() != null ? board : null)
                .category(req.getCategory())
                .question(req.getQuestion())
                .answer(req.getAnswer())
                .sortOrder(req.getSortOrder() == null ? 0 : req.getSortOrder())
                .enabled(req.getEnabled() == null ? true : req.getEnabled())
                .build();
        faq = repo.save(faq);
        return toDto(faq);
    }

    public Page<FaqResponse> list(String code, String category, Pageable pageable) {
        if (category != null && !category.isBlank()) {
            return repo.findByBoard_CodeAndCategoryAndEnabledTrueOrderBySortOrderAsc(code, category, pageable)
                    .map(this::toDto);
        }
        return repo.findByBoard_CodeAndEnabledTrueOrderBySortOrderAsc(code, pageable)
                .map(this::toDto);
    }

    public FaqResponse get(Long id) {
        FaqEntity faq = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("FAQ 없음: id=" + id));
        return toDto(faq);
    }

    @Transactional
    public FaqResponse update(Long id, FaqUpdateRequest req) {
        FaqEntity faq = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("FAQ 없음: id=" + id));

        if (req.getCategory() != null) faq.setCategory(req.getCategory());
        if (req.getQuestion() != null) faq.setQuestion(req.getQuestion());
        if (req.getAnswer() != null) faq.setAnswer(req.getAnswer());
        if (req.getSortOrder() != null) faq.setSortOrder(req.getSortOrder());
        if (req.getEnabled() != null) faq.setEnabled(req.getEnabled());

        return toDto(faq);
    }

    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new NotFoundException("FAQ 없음: id=" + id);
        }
        repo.deleteById(id);
    }

    private FaqResponse toDto(FaqEntity faq) {
        return FaqResponse.builder()
                .id(faq.getId())
                .code(faq.getCode().getCode())
                .category(faq.getCategory())
                .question(faq.getQuestion())
                .answer(faq.getAnswer())
                .sortOrder(faq.getSortOrder())
                .enabled(faq.isEnabled())
                .createdAt(faq.getCreatedAt())
                .updatedAt(faq.getUpdatedAt())
                .build();
    }
}
