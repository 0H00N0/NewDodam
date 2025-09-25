package com.dodam.admin.service;

import com.dodam.admin.dto.MemberResponseDTO;
import com.dodam.member.repository.MemberRepository; // 'MemberRepository'는 미리 생성되어 있어야 합니다.
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMemberService {

    private final MemberRepository memberRepository;

    /**
     * 모든 회원 목록 조회
     * @return 회원 정보 DTO 리스트
     */
    public List<MemberResponseDTO> findAllMembers() {
        return memberRepository.findAll().stream()
                .map(MemberResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 특정 회원 상세 조회
     * @param mnum 회원 ID
     * @return 회원 정보 DTO
     */
    public MemberResponseDTO findMemberById(Long mnum) {
        return memberRepository.findById(mnum)
                .map(MemberResponseDTO::fromEntity)
                .orElseThrow(() -> new EntityNotFoundException("회원을 찾을 수 없습니다. ID: " + mnum));
    }

    /**
     * 회원 삭제 (강제 탈퇴)
     * @param mnum 삭제할 회원 ID
     */
    @Transactional
    public void deleteMember(Long mnum) {
        if (!memberRepository.existsById(mnum)) {
            throw new EntityNotFoundException("삭제할 회원을 찾을 수 없습니다. ID: " + mnum);
        }
        memberRepository.deleteById(mnum);
    }
}