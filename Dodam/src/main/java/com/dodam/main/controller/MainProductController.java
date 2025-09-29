package com.dodam.main.controller;

import com.dodam.main.dto.MainNewProductByNameDTO;
import com.dodam.main.dto.MainPopularProductByNameDTO;
import com.dodam.main.service.MainProductQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class MainProductController {

    private final MainProductQueryService svc;

    /** 신상품 */
    @GetMapping("/new")
    public List<MainNewProductByNameDTO> getNewProductsByName(
            @RequestParam(name = "limit", required = false, defaultValue = "12") int limit
    ) {
        return svc.getNewProductsByName(limit);
    }

    /** 인기상품 */
    @GetMapping("/popular")
    public List<MainPopularProductByNameDTO> getPopularProductsByName(
            @RequestParam(name = "limit", required = false, defaultValue = "12") int limit
    ) {
        return svc.getPopularProductsByName(limit);
    }
}
