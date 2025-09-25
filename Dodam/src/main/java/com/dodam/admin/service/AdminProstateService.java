package com.dodam.admin.service;

import com.dodam.admin.dto.ProstateResponseDTO;
import com.dodam.product.repository.ProstateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminProstateService {

    private final ProstateRepository prostateRepository;

    @Transactional(readOnly = true)
    public List<ProstateResponseDTO> findAllProstates() {
        return prostateRepository.findAll().stream()
                .map(ProstateResponseDTO::new)
                .collect(Collectors.toList());
    }
}