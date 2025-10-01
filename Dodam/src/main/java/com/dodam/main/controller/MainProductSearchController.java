// src/main/java/com/dodam/main/controller/MainProductSearchController.java
package com.dodam.main.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;

import com.dodam.main.dto.MainProductSearchDTO;
import com.dodam.main.service.MainProductSearchService;

import java.math.BigDecimal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class MainProductSearchController {

    private final MainProductSearchService service;

    /**
     * 예:
     * /api/products/search?q=뽀로로&ageMin=3&ageMax=5&priceMin=10000&priceMax=30000&sort=PRICE_ASC&page=0&size=24
     *
     * sort: RECENT | PRICE_ASC | PRICE_DESC (기본 RECENT)
     */
    @GetMapping("/search")
    public Page<MainProductSearchDTO> search(
        @RequestParam(name = "q", required = false) String q,
        @RequestParam(name = "ageMin", required = false) Integer ageMin,
        @RequestParam(name = "ageMax", required = false) Integer ageMax,
        @RequestParam(name = "priceMin", required = false) BigDecimal priceMin,
        @RequestParam(name = "priceMax", required = false) BigDecimal priceMax,
        @RequestParam(name = "sort", defaultValue = "RECENT") String sort,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "24") int size
    ) {
        Sort s = switch (sort) {
            case "PRICE_ASC"  -> Sort.by(Sort.Direction.ASC,  "proborrow", "procre");
            case "PRICE_DESC" -> Sort.by(Sort.Direction.DESC, "proborrow", "procre");
            default           -> Sort.by(Sort.Direction.DESC, "procre"); // RECENT
        };
        Pageable pageable = PageRequest.of(page, size, s);

        return service.search(q, ageMin, ageMax, priceMin, priceMax, pageable);
    }
}
