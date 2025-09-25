package com.dodam.admin.service;

import com.dodam.admin.dto.VocAdminDto;
import com.dodam.member.entity.MemberEntity;
import com.dodam.member.repository.MemberRepository; // MemberRepository가 있다고 가정
import com.dodam.voc.entity.VocAnswerEntity;
import com.dodam.voc.entity.VocEntity;
import com.dodam.voc.repository.VocAnswerRepository;
import com.dodam.voc.repository.VocRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VocAdminService {

    private final VocRepository vocRepository;
    private final VocAnswerRepository vocAnswerRepository;
    private final MemberRepository memberRepository; // 담당자 지정을 위해 추가

    /**
     * 모든 VOC 목록 조회 (페이징)
     */
    public Page<VocAdminDto.VocListResponse> getAllVocs(Pageable pageable) {
        return vocRepository.findAll(pageable)
                .map(VocAdminDto.VocListResponse::fromEntity);
    }

    /**
     * 특정 VOC 상세 조회
     */
    public VocAdminDto.VocDetailResponse getVocById(Long vocId) {
        VocEntity voc = vocRepository.findById(vocId)
                .orElseThrow(() -> new EntityNotFoundException("VOC not found with id: " + vocId));
        return VocAdminDto.VocDetailResponse.fromEntity(voc);
    }

    /**
     * VOC 답변 등록 및 상태 변경 (수정된 로직)
     */
    @Transactional
    public VocAdminDto.VocDetailResponse updateVoc(Long vocId, VocAdminDto.VocUpdateRequest request) {
        VocEntity voc = vocRepository.findById(vocId)
                .orElseThrow(() -> new EntityNotFoundException("VOC not found with id: " + vocId));

        // 1. 답변 처리
        if (request.getAnswerContent() != null && !request.getAnswerContent().isBlank()) {
            VocAnswerEntity answer = voc.getAnswer();
            if (answer == null) {
                // 새 답변 생성 시, voc와의 관계를 설정
                answer = VocAnswerEntity.builder()
                        .voc(voc)
                        .content(request.getAnswerContent())
                        .build();
                voc.setAnswer(answer); // voc에 답변을 설정 (가장 중요!)
            } else {
                // 기존 답변 수정
                answer.setContent(request.getAnswerContent());
            }
        }

        // 2. 상태 변경
        if (request.getStatus() != null) {
            voc.setStatus(request.getStatus());
        }

        // 3. 담당자 할당
        if (request.getHandlerMnum() != null) {
            MemberEntity handler = memberRepository.findById(request.getHandlerMnum())
                    .orElseThrow(() -> new EntityNotFoundException("Handler not found with mnum: " + request.getHandlerMnum()));
            voc.setHandler(handler);
        }

        // voc만 저장하면 Cascade 설정에 의해 answer도 함께 처리됨
        VocEntity savedVoc = vocRepository.save(voc);

        return VocAdminDto.VocDetailResponse.fromEntity(savedVoc);
    }
    
}