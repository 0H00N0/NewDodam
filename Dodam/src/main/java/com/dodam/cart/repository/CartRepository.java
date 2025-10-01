package com.dodam.cart.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dodam.cart.Entity.CartEntity;

public interface CartRepository extends JpaRepository<CartEntity, Long> {
    
}