package com.dodam.admin.service;

import com.dodam.admin.dto.MemberResponseDTO;
import com.dodam.member.entity.MemberEntity;
import com.dodam.member.entity.MemberEntity.MemStatus;
import com.dodam.member.repository.MemberRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMemberService {

    private final MemberRepository memberRepository;

    /**
     * 모든 회원 목록 조회
     */
    public List<MemberResponseDTO> findAllMembers() {
        return memberRepository.findAll().stream()
                .map(MemberResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 상태별 회원 조회 (ACTIVE / DELETED)
     */
    public List<MemberResponseDTO> findMembersByStatus(MemStatus status) {
        return memberRepository.findByMemstatus(status).stream()
                .map(MemberResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 특정 회원 상세 조회
     */
    public MemberResponseDTO findMemberById(Long mnum) {
        return memberRepository.findById(mnum)
                .map(MemberResponseDTO::fromEntity)
                .orElseThrow(() -> new EntityNotFoundException("회원을 찾을 수 없습니다. ID: " + mnum));
    }

    /**
     * 회원 삭제 (강제 탈퇴 → 상태 변경)
     */
    @Transactional
    public void deleteMember(Long mnum) {
        MemberEntity member = memberRepository.findById(mnum)
                .orElseThrow(() -> new EntityNotFoundException("삭제할 회원을 찾을 수 없습니다. ID: " + mnum));

        member.setMemstatus(MemStatus.DELETED);       // ✅ 상태 변경
        member.setDeletedAt(LocalDateTime.now());     // ✅ 탈퇴 시간 기록
        member.setDeletedReason("관리자 강제 탈퇴");     // ✅ 기본 사유 저장
    }
}
