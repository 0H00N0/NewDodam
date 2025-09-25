package com.dodam.member.repository;

import com.dodam.member.entity.ChildEntity;
import com.dodam.member.entity.MemberEntity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChildRepository extends JpaRepository<ChildEntity, Long> {
	
	void deleteByMember(MemberEntity member);
}