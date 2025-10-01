// src/main/java/com/dodam/main/service/MainProductSearchService.java
package com.dodam.main.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.dodam.main.dto.MainProductSearchDTO;

import java.math.BigDecimal;

public interface MainProductSearchService {
    Page<MainProductSearchDTO> search(
            String q,
            Integer ageMin, Integer ageMax,
            BigDecimal priceMin, BigDecimal priceMax,
            Pageable pageable
    );
}
