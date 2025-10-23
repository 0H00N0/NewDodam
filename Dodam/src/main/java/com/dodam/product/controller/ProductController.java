package com.dodam.product.controller;

import com.dodam.product.dto.ProductDTO;
import com.dodam.product.service.ProductService;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.*;
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
	
	 private static final Logger log = LoggerFactory.getLogger(ProductController.class);
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
    
    @GetMapping("/{id}/images")
    public ResponseEntity<?> getImages(
            @PathVariable("id") Long id,
            @RequestParam(value = "limit", required = false, defaultValue = "1") Integer limit,
            @RequestHeader Map<String, String> headers) {

        log.info("GET /api/products/{}/images called. limit={}, headers={}", id, limit, headers.keySet());

        try {
            List<String> urls = productService.getProductImageUrls(id, limit);
            if (urls == null || urls.isEmpty()) {
                log.info("No images for product id={}", id);
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(urls);
        } catch (IllegalArgumentException ex) {
            log.warn("IllegalArgument for id={}: {}", id, ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.emptyList());
        } catch (Exception ex) {
            log.error("Error while fetching images for id=" + id, ex);
            // 에러 디테일(개발 중) 반환
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "internal_error", "message", ex.getMessage()));
        }
    }
}
    

