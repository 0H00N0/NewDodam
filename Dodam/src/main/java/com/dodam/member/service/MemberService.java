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

import java.time.LocalDate;
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
        // (선택) 로직 차원에서도 재검증
        if (dto.getMbirth() != null && dto.getMbirth().isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "생년월일은 미래일 수 없습니다.");
        }
        if (dto.getChildren() != null) {
            for (ChildDTO ch : dto.getChildren()) {
                if (ch.getChbirth() != null && ch.getChbirth().isAfter(LocalDate.now())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "자녀 생년월일은 미래일 수 없습니다.");
                }
                if (ch.getChbirth() != null && ch.getChbirth().isBefore(LocalDate.of(2000, 1, 1))) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "자녀 생년월일은 2000-01-01 이후여야 합니다.");
                }
            }
        }

        // ✅ (중요) ACTIVE 기준 중복 체크 — 재가입 허용 위해
        memberRepository.findByMidAndMemstatus(dto.getMid(), MemberEntity.MemStatus.ACTIVE)
            .ifPresent(m -> { throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다."); });

        // ✅ [신규] 전화번호 정규화(숫자만) + ACTIVE 중복 차단
        String mtelDigits = dto.getMtel() == null ? null : dto.getMtel().replaceAll("\\D", "");
        dto.setMtel(mtelDigits); // 저장 일관성 유지
        if (mtelDigits != null && !mtelDigits.isBlank()) {
            boolean telDup = memberRepository.existsByMtelAndMemstatus(mtelDigits, MemberEntity.MemStatus.ACTIVE);
            if (telDup) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "해당 전화번호는 이미 사용 중입니다. 회원가입을 하셨다면 아이디 및 비밀번호를 찾아주세요.");
            }
        }

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
                .mtel(dto.getMtel())          // ← 정규화된 값 저장
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "아이디 혹은 비밀번호가 맞지 않습니다."));

        // ✅ 해시 검증
        if (!passwordEncoder.matches(rawPw, e.getMpw())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "아이디 혹은 비밀번호가 맞지 않습니다.");
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

        // ✅ [신규] 전화번호 변경 시: 숫자만 보관 + ACTIVE 중복 차단
        String newMtel = dto.getMtel() == null ? null : dto.getMtel().replaceAll("\\D", "");
        if (newMtel != null && !newMtel.equals(entity.getMtel())) {
            boolean telDup = memberRepository.existsByMtelAndMemstatus(newMtel, MemberEntity.MemStatus.ACTIVE);
            if (telDup) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "해당 전화번호는 이미 사용 중입니다. 회원가입을 하셨다면 아이디 및 비밀번호를 찾아주세요.");
            }
        }

        entity.setMemail(dto.getMemail());
        entity.setMtel(newMtel);              // ← 정규화 반영
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

        // ✅ 안전 마스킹 (길이/UNIQUE/Oracle "" 주의)
        String suf36 = Long.toString(m.getMnum(), 36);
        int last3 = (int)(m.getMnum() % 1000);

        // 👇 방식 A: mid도 유일하게 변경 (UNIQUE 잠금 해제)
        m.setMid("deleted_" + suf36);

        m.setMname("탈퇴한 사용자");
        m.setMnic("d-" + suf36);
        m.setMtel(String.format("000-0000-%03d", last3)); // 13자 고정
        m.setMaddr("-");                                  // "" 금지(Oracle)
        m.setMpost(0L);
        m.setMemail("deleted+" + suf36 + "@invalid.local");

        m.setMemstatus(MemberEntity.MemStatus.DELETED);
        m.setDeletedAt(LocalDateTime.now());
        m.setDeletedReason((reasonOrNull != null && !reasonOrNull.isBlank()) ? reasonOrNull.trim() : null);

        memberRepository.save(m);
    }
    
    //mid로 회원번호 탐색
    public Long findMnumByMid(String mid) {
        return memberRepository.findByMid(mid)
            .map(MemberEntity::getMnum)
            .orElse(null);
    }

}
