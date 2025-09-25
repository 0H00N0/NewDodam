package com.dodam.voc.repository;

import com.dodam.member.entity.MemberEntity;
import com.dodam.voc.entity.VocEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VocRepository extends JpaRepository<VocEntity, Long> {

    // 특정 회원이 작성한 모든 VOC 목록 조회 (작성일 내림차순)
    List<VocEntity> findByAuthorOrderByCreatedAtDesc(MemberEntity author);

    // 특정 담당자에게 할당된 모든 VOC 목록 조회
    List<VocEntity> findByHandler(MemberEntity handler);

    // 제목으로 VOC 검색 (페이징 처리 포함)
    Page<VocEntity> findByTitleContaining(String title, Pageable pageable);

    // 카테고리로 VOC 검색 (페이징 처리 포함)
    Page<VocEntity> findByCategory(VocEntity.VocCategory category, Pageable pageable);

}