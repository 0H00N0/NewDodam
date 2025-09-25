package com.dodam.member.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.dodam.member.entity.LoginmethodEntity;

public interface LoginmethodRepository extends JpaRepository<LoginmethodEntity, Long> {
    Optional<LoginmethodEntity> findByLmtype(String lmtype);
}
