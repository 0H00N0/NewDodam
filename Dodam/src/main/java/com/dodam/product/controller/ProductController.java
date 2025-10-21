package com.dodam.product.controller;

import com.dodam.product.dto.ProductDTO;
import com.dodam.product.service.ProductService;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;

import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:3000") // (포트 3000에서 오는 요청 허용)
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<?> list(
        @RequestParam(name = "q", required = false) String q,
        @RequestParam(name = "catenum", required = false) Long catenum,
        @RequestParam(name = "prosnum", required = false) Long prosnum,
        @RequestParam(name = "prograde", required = false) String prograde,
        @PageableDefault(size = 20, sort = "pronum", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        System.out.println("==== /products API 호출됨 ====");
        Page<ProductDTO> result = productService.searchByColumns(q, catenum, prosnum, prograde, pageable);
        if (result.isEmpty()) {
            return ResponseEntity.ok("등록된 상품이 없습니다.");
        }
        return ResponseEntity.ok(result);
        }

    @GetMapping("/{pronum}")
    public ProductDTO get(@PathVariable("pronum") Long pronum) {
        return productService.get(pronum);
    }

    
    @PostMapping
    public ProductDTO create(@RequestBody ProductDTO dto) {
        return productService.create(dto);
    }

    @PutMapping("/{pronum}")
    public ProductDTO update(@PathVariable("pronum") Long pronum, @RequestBody ProductDTO dto) {
        dto.setPronum(pronum);
        return productService.update(dto);
    }

    @DeleteMapping("/{pronum}")
    public void delete(@PathVariable("pronum") Long pronum) {
        productService.delete(pronum);
    }
    
    @GetMapping("/category/{categoryName}")
    public ResponseEntity<?> getByCategory(@PathVariable("categoryName") String categoryName) {
        System.out.println(">>> getByCategory called: " + categoryName);
        try {
            return ResponseEntity.ok(productService.findByCategoryName(categoryName));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @GetMapping("/{proId}/images")
    public ResponseEntity<List<String>> getProductImages(
            @PathVariable("proId") Long proId,
            @RequestParam(name = "limit", required = false) Integer limit
    ) {
        try {
            List<String> urls = productService.getProductImageUrls(proId, limit);
            if (urls == null || urls.isEmpty()) return ResponseEntity.noContent().build();
            return ResponseEntity.ok(urls);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.emptyList());
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
}
