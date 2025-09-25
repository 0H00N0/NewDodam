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
        if (memberRepository.existsByMid(dto.getMid())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "duplicated mid");
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
                .mtel(dto.getMtel())
                .loginmethod(getOrCreateLocal())
                .memtype(getOrCreateDefault())
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
        var e = memberRepository.findByMid(mid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid id/pw"));

        // ✅ 해시 검증
        if (!passwordEncoder.matches(rawPw, e.getMpw())) {
            // (선택) 평문→해시 마이그레이션이 필요하면 아래 주석 블록 참고
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid id/pw");
        }

        return new MemberDTO(e);
    }

    public boolean exists(String mid) {
        return memberRepository.existsByMid(mid);
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }

    @Transactional
    public void updateProfile(String sid, MemberDTO dto) {
        MemberEntity entity = memberRepository.findByMid(sid)
            .orElseThrow(() -> new RuntimeException("회원 없음"));
        entity.setMemail(dto.getMemail());
        entity.setMtel(dto.getMtel());
        entity.setMaddr(dto.getMaddr());
        entity.setMnic(dto.getMnic());
        entity.setMpost(dto.getMpost());
        entity.setMname(dto.getMname());
        entity.setMbirth(dto.getMbirth());
        memberRepository.save(entity);
        //자녀정보 삭제 후 정보 재삽입
        childRepository.deleteByMember(entity);
        if (dto.getChildren() != null) {
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
        MemberEntity entity = memberRepository.findByMid(sid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원 없음"));

        // 현재 비밀번호 검증
        if (!passwordEncoder.matches(dto.getCurrentPw(), entity.getMpw())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "현재 비밀번호가 일치하지 않습니다.");
        }
        entity.setMpw(passwordEncoder.encode(dto.getNewPw()));

        // 새 비밀번호 저장
        entity.setMpw(passwordEncoder.encode(dto.getNewPw()));
        memberRepository.save(entity);
    }

    public MemberDTO me(String mid) {
        var e = memberRepository.findByMid(mid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원 없음"));
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
        MemberEntity member = memberRepository.findByMid(mid).orElseThrow();
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
        MemberEntity entity = memberRepository.findByMid(mid)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원 없음"));
        entity.setMpw(passwordEncoder.encode(newPw));
        memberRepository.save(entity);
    }
    
    public MemberDTO findByMid(String mid) {
        var e = memberRepository.findByMid(mid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "no member"));
        return new MemberDTO(e);
    }

    
}
