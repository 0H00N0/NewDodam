package com.dodam.main.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;

import com.dodam.main.dto.MainProductSearchDTO;
import com.dodam.main.service.MainProductSearchService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class MainProductSearchController {

    private final MainProductSearchService service;

    /** 예: /api/products/search?q=뽀로로&sort=RECENT&page=0&size=24 */
    @GetMapping("/search")
    public Page<MainProductSearchDTO> search(
        @RequestParam(name = "q", required = false) String q,
        @RequestParam(name = "sort", defaultValue = "RECENT") String sort,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "24") int size
    ) {
        Sort s = switch (sort) {
            case "PRICE_ASC"  -> Sort.by(Sort.Direction.ASC,  "proborrow", "procre");
            case "PRICE_DESC" -> Sort.by(Sort.Direction.DESC, "proborrow", "procre");
            default           -> Sort.by(Sort.Direction.DESC, "procre");
        };
        Pageable pageable = PageRequest.of(page, size, s);
        return service.searchByName(q, pageable);
    }
}
