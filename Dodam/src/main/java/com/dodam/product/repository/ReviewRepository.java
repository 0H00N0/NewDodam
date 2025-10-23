package com.dodam.product.repository;

import com.dodam.member.entity.MemberEntity;
import com.dodam.product.entity.ProductEntity;
import com.dodam.product.entity.ReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {
    List<ReviewEntity> findAllByMemberOrderByRevnumDesc(MemberEntity member);

    List<ReviewEntity> findByProductOrderByRevcreDesc(ProductEntity product);
}
