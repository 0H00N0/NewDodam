package com.dodam.inquiry.repository;

import com.dodam.inquiry.entity.ProductInquiryEntity;
import org.springframework.data.jpa.repository.*;
import java.util.List;

public interface ProductInquiryRepository extends JpaRepository<ProductInquiryEntity, Long> {
  @EntityGraph(attributePaths = {"product"})
  List<ProductInquiryEntity> findByMember_MidOrderByIdDesc(String mid);
}
