package com.dodam.member.service;

import com.dodam.member.dto.ChangePwDTO;
import com.dodam.member.dto.ChildDTO;
import com.dodam.member.dto.MemberDTO;
import com.dodam.member.entity.ChildEntity;
import com.dodam.member.entity.LoginmethodEntity;
import com.dodam.member.entity.MemberEntity;
import com.dodam.member.entity.MemtypeEntity;
import com.dodam.member.repository.ChildRepository;
import com.dodam.member.repository.LoginmethodRepository;
import com.dodam.member.repository.MemberRepository;
import com.dodam.member.repository.MemtypeRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder; // ✅ 추가
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final LoginmethodRepository loginmethodRepository;
    private final MemtypeRepository memtypeRepository;
    private final PasswordEncoder passwordEncoder; 
    private final ChildRepository childRepository;

    private LoginmethodEntity getOrCreateLocal() {
        return loginmethodRepository.findByLmtype("LOCAL")
                .orElseGet(() -> loginmethodRepository.save(
                        LoginmethodEntity.builder().lmtype("LOCAL").build()
                ));
    }

    private MemtypeEntity getOrCreateDefault() {
        // 0 = 일반
        return memtypeRepository.findByMtcode(0)
                .orElseGet(() -> memtypeRepository.save(
                        MemtypeEntity.builder().mtcode(0).mtname("일반").build()
                ));
    }

    public void signup(MemberDTO dto) {
        // ✅ (중요) ACTIVE 기준 중복 체크 — 재가입 허용 위해
        memberRepository.findByMidAndMemstatus(dto.getMid(), MemberEntity.MemStatus.ACTIVE)
            .ifPresent(m -> { throw new ResponseStatusException(HttpStatus.CONFLICT, "duplicated mid"); });

        // ✅ 비밀번호 해시 저장
        String encoded = passwordEncoder.encode(dto.getMpw());

        MemberEntity e = MemberEntity.builder()
                .mid(dto.getMid())
                .mpw(encoded)                  // ✅ 해시 저장
                .mname(dto.getMname())
                .mpost(dto.getMpost())
                .maddr(dto.getMaddr())
                .memail(dto.getMemail())
                .mbirth(dto.getMbirth())
                .mnic(dto.getMnic())
                .mtel(dto.getMtel())
                .loginmethod(getOrCreateLocal())
                .memtype(getOrCreateDefault())
                .memstatus(MemberEntity.MemStatus.ACTIVE) // ✅ 기본 ACTIVE
                .build();

        memberRepository.save(e);

        //자녀정보 저장
        if (dto.getChildren() != null && !dto.getChildren().isEmpty()) {
            for (ChildDTO c : dto.getChildren()) {
                ChildEntity child = ChildEntity.builder()
                        .chname(c.getChname())
                        .chbirth(c.getChbirth())
                        .member(e)
                        .build();
                childRepository.save(child);
            }
        }
    }

    public MemberDTO login(String mid, String rawPw) {
        // ✅ ACTIVE만 로그인 허용
        var e = memberRepository.findByMidAndMemstatus(mid, MemberEntity.MemStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid id/pw"));

        // ✅ 해시 검증
        if (!passwordEncoder.matches(rawPw, e.getMpw())) {
            // (선택) 평문→해시 마이그레이션이 필요하면 아래 주석 블록 참고
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid id/pw");
        }

        return new MemberDTO(e);
    }

    public boolean exists(String mid) {
        // ✅ ACTIVE만 기준
        return memberRepository.findByMidAndMemstatus(mid, MemberEntity.MemStatus.ACTIVE).isPresent();
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }

    @Transactional
    public void updateProfile(String sid, MemberDTO dto) {
        // ✅ ACTIVE만 수정 허용
        MemberEntity entity = memberRepository.findByMidAndMemstatus(sid, MemberEntity.MemStatus.ACTIVE)
            .orElseThrow(() -> new RuntimeException("회원 없음 또는 탈퇴"));

        entity.setMemail(dto.getMemail());
        entity.setMtel(dto.getMtel());
        entity.setMaddr(dto.getMaddr());
        entity.setMnic(dto.getMnic());
        entity.setMpost(dto.getMpost());
        entity.setMname(dto.getMname());
        entity.setMbirth(dto.getMbirth());
        memberRepository.save(entity);

        // 자녀정보 삭제 후 정보 재삽입
        childRepository.deleteByMember(entity);

        if (dto.getChildren() != null && !dto.getChildren().isEmpty()) {
            for (ChildDTO c : dto.getChildren()) {
                ChildEntity child = ChildEntity.builder()
                    .chname(c.getChname())
                    .chbirth(c.getChbirth())
                    .member(entity)
                    .build();
                childRepository.save(child);
            }
        }

    }

    public void changePw(String sid, ChangePwDTO dto) {
        // ✅ ACTIVE만 변경 허용
        MemberEntity entity = memberRepository.findByMidAndMemstatus(sid, MemberEntity.MemStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원 없음 또는 탈퇴"));

        // 현재 비밀번호 검증
        if (!passwordEncoder.matches(dto.getCurrentPw(), entity.getMpw())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "현재 비밀번호가 일치하지 않습니다.");
        }
        

        // 새 비밀번호 저장 (기존 주석/흐름 유지)
        entity.setMpw(passwordEncoder.encode(dto.getNewPw()));
        memberRepository.save(entity);
    }

    public MemberDTO me(String mid) {
        // ✅ ACTIVE만 조회
        var e = memberRepository.findByMidAndMemstatus(mid, MemberEntity.MemStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원 없음 또는 탈퇴"));
        return new MemberDTO(e);
    }

    public String findIdByNameAndTel(String mname, String mtel) {
        return memberRepository.findByMnameAndMtel(mname, mtel)
            .map(MemberEntity::getMid)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "일치하는 회원이 없습니다."));
    }

    public String findIdByNameAndEmail(String mname, String memail) {
        return memberRepository.findByMnameAndMemail(mname, memail)
            .map(MemberEntity::getMid)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "일치하는 회원이 없습니다."));
    }

    // 비밀번호 암호화 후 DB에 저장
    public void updatePassword(String mid, String tempPw) {
        MemberEntity member = memberRepository.findByMidAndMemstatus(mid, MemberEntity.MemStatus.ACTIVE)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원 없음 또는 탈퇴"));
        member.setMpw(passwordEncoder.encode(tempPw));
        memberRepository.save(member);
    }

    public boolean existsByMidNameEmail(String mid, String mname, String memail) {
        return memberRepository.findByMidAndMnameAndMemail(mid, mname, memail).isPresent();
    }

    public boolean existsByMidNameTel(String mid, String mname, String mtel) {
        return memberRepository.findByMidAndMnameAndMtel(mid, mname, mtel).isPresent();
    }
    
    public void changePwDirect(String mid, String newPw) {
        MemberEntity entity = memberRepository.findByMidAndMemstatus(mid, MemberEntity.MemStatus.ACTIVE)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원 없음 또는 탈퇴"));
        entity.setMpw(passwordEncoder.encode(newPw));
        memberRepository.save(entity);
    }
    
    public MemberDTO findByMid(String mid) {
        var e = memberRepository.findByMidAndMemstatus(mid, MemberEntity.MemStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "no member or deleted"));
        return new MemberDTO(e);
    }

    // ✅ (신규) 탈퇴: PII 마스킹 + 랜덤 해시 + 소셜 provider 해제 + 상태 전환
    @Transactional
    public void deleteAccount(String mid, String confirmPwOrNull, String reasonOrNull) {
        MemberEntity m = memberRepository.findByMidAndMemstatus(mid, MemberEntity.MemStatus.ACTIVE)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원 없음/이미 탈퇴"));

        boolean isLocal = (m.getLoginmethod() == null) ||
                          "LOCAL".equalsIgnoreCase(m.getLoginmethod().getLmtype());
        if (isLocal) {
            if (confirmPwOrNull == null || m.getMpw() == null || !passwordEncoder.matches(confirmPwOrNull, m.getMpw())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다.");
            }
        }

        // 로그인 차단
        m.setMpw(passwordEncoder.encode(UUID.randomUUID().toString()));

        // ✅ PK(mnum)로 유니크 보장되는 마스킹(UNIQUE 제약 충돌 방지)
        String suf = String.valueOf(m.getMnum());
        m.setMname("탈퇴한 사용자");
        m.setMnic("deleted-" + suf);                 // 닉네임 UNIQUE면 충돌 방지
        m.setMtel("000-0000-" + suf);                // 전화번호 UNIQUE면 충돌 방지
        m.setMaddr("");
        m.setMpost(0L);
        m.setMemail("deleted+" + suf + "@invalid.local"); // 이메일 UNIQUE면 충돌 방지

        // 소셜 연결 해제 필요 시 추가 로직 위치

        m.setMemstatus(MemberEntity.MemStatus.DELETED);
        m.setDeletedAt(LocalDateTime.now());
        m.setDeletedReason(
            (reasonOrNull != null && !reasonOrNull.isBlank()) ? reasonOrNull.trim() : null
        );
        

        memberRepository.save(m);
    }
}
