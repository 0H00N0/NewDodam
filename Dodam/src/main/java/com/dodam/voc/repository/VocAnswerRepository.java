package com.dodam.voc.repository;

import com.dodam.voc.entity.VocAnswerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VocAnswerRepository extends JpaRepository<VocAnswerEntity, Long> {
    // VocAnswer는 대부분 Voc를 통해 접근하므로 복잡한 쿼리가 필요 없는 경우가 많습니다.
    // 필요에 따라 쿼리 메소드를 추가할 수 있습니다.
}