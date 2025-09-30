package com.dodam.main.controller;

import com.dodam.main.dto.MainNewProductByNameDTO;
import com.dodam.main.dto.MainPopularProductByNameDTO;
import com.dodam.main.dto.MainProductBasicDTO;
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

    /** ✅ 단건 상세 (히어로에서 상세 이동용) */
    @GetMapping("/{proId}")
    public MainProductBasicDTO getProductBasic(@PathVariable Long proId) {
        return svc.getProductBasic(proId);
    }
    
    /** 특정 상품(=PRONUM)의 상세 이미지 URL 목록 */
    @GetMapping("/{proId}/images")
    public List<String> getProductImages(
            @PathVariable Long proId,
            @RequestParam(name = "limit", required = false) Integer limit
    ) {
        return svc.getProductImageUrls(proId, limit);
    }
}
