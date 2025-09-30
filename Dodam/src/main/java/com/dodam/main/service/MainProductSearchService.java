package com.dodam.main.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.dodam.main.dto.MainProductSearchDTO;

public interface MainProductSearchService {
    Page<MainProductSearchDTO> searchByName(String q, Pageable pageable);
}
