package com.dodam.cart.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.dodam.cart.Entity.CartEntity;

public interface CartRepository extends JpaRepository<CartEntity, Long> {
    List<CartEntity> findByMnum(Long mnum);
    Optional<CartEntity> findByMnumAndPronum(Long mnum, Long pronum);
    
    // ✅ 추가
    void deleteByMnumAndPronum(Long mnum, Long pronum);
    void deleteByMnum(Long mnum);
}
