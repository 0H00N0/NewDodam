package com.dodam.member.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.dodam.member.entity.MemberEntity;

public interface MemberRepository extends JpaRepository<MemberEntity, Long> {
	boolean existsByMid(String mid);
	Optional<MemberEntity> findByMid(String mid);
	
	// 이름+전화번호로 찾기
	Optional<MemberEntity> findByMnameAndMtel(String mname, String mtel);

	// 이름+이메일로 찾기
	Optional<MemberEntity> findByMnameAndMemail(String mname, String memail);
	
	// 이메일로 비밀번호 찾기
	Optional<MemberEntity> findByMidAndMnameAndMemail(String mid, String mname, String memail);

	//전화번호로 비밀번호 찾기
	Optional<MemberEntity> findByMidAndMnameAndMtel(String mid, String mname, String mtel);


}

