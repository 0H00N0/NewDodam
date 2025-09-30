package com.dodam.main.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MainProductSearchDTO(
        Long id,             // pronum
        String name,         // proname
        BigDecimal price,    // proborrow (대여가)
        String thumbnailUrl, // 첫 이미지 prourl
        LocalDateTime createdAt
) {}
