package com.dodam.admin.repository;

import com.dodam.admin.entity.AdminEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<AdminEntity, Long> {
    
    /**
     * 사용자명으로 관리자 조회
     */
    Optional<AdminEntity> findByUsername(String username);
    
    /**
     * 활성화된 관리자만 조회
     */
    Optional<AdminEntity> findByUsernameAndEnabledTrue(String username);
}