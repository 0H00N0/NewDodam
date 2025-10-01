package com.dodam.member.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.dodam.member.entity.MemberEntity;
import com.dodam.member.entity.MemberEntity.MemStatus;

public interface MemberRepository extends JpaRepository<MemberEntity, Long> {
    boolean existsByMid(String mid);
    Optional<MemberEntity> findByMid(String mid);

    // ✅ ACTIVE 전용 조회 (로그인/내정보/수정 등 보호 경로에서 사용)
    Optional<MemberEntity> findByMidAndMemstatus(String mid, MemberEntity.MemStatus memstatus);
    
    // ✅ ACTIVE에서만 중복 판단용 — 더 가벼움
    boolean existsByMidAndMemstatus(String mid, MemberEntity.MemStatus memstatus);

    // 이름+전화번호로 찾기
    Optional<MemberEntity> findByMnameAndMtel(String mname, String mtel);

    // 이름+이메일로 찾기
    Optional<MemberEntity> findByMnameAndMemail(String mname, String memail);

    // 이메일로 비밀번호 찾기
    Optional<MemberEntity> findByMidAndMnameAndMemail(String mid, String mname, String memail);

    //전화번호로 비밀번호 찾기
    Optional<MemberEntity> findByMidAndMnameAndMtel(String mid, String mname, String mtel);
    
    //관리자용 회원 상태별 조회
    List<MemberEntity> findByMemstatus(MemStatus status);

}
