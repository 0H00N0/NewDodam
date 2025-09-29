package com.dodam.admin.controller;

import com.dodam.admin.dto.AdminProductRequestDTO;
import com.dodam.admin.dto.ProductDetailResponseDTO;
import com.dodam.admin.dto.ProductListResponseDTO;
import com.dodam.admin.service.AdminProductService;
import com.dodam.product.entity.ProductEntity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController 
@RequestMapping("/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final AdminProductService adminProductService;

    /**
     * ✅ 단일 상품 등록
     * - DTO 안에서 바로 이미지 URL을 받아 DB에 저장
     */
    @PostMapping
    public ResponseEntity<?> registerProduct(@Valid @RequestBody AdminProductRequestDTO requestDTO) {
        ProductEntity createdProduct = adminProductService.createProduct(requestDTO);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("pronum", createdProduct.getPronum()));
    }

    /**
     * ✅ 전체 상품 조회
     */
    @GetMapping
    public ResponseEntity<List<ProductListResponseDTO>> getAllProducts() {
        List<ProductListResponseDTO> products = adminProductService.findAllProducts();
        return ResponseEntity.ok(products);
    }

    /**
     * ✅ 단일 상품 상세 조회
     */
    @GetMapping("/{productId}")
    public ResponseEntity<ProductDetailResponseDTO> getProductById(@PathVariable("productId") Long productId) {
        ProductDetailResponseDTO product = adminProductService.findProductById(productId);
        return ResponseEntity.ok(product);
    }

    /**
     * ✅ 단일 상품 수정
     */
    @PutMapping("/{productId}")
    public ResponseEntity<ProductDetailResponseDTO> updateProduct(
            @PathVariable("productId") Long productId,
            @Valid @RequestBody AdminProductRequestDTO requestDTO) {
        
        ProductDetailResponseDTO updatedProduct = adminProductService.updateProduct(productId, requestDTO);
        return ResponseEntity.ok(updatedProduct);
    }

    /**
     * ✅ 단일 상품 삭제
     */
    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(@PathVariable("productId") Long productId) {
        adminProductService.deleteProduct(productId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bulk-upload")
    public ResponseEntity<?> bulkUploadProducts(@RequestParam("file") MultipartFile csvFile) {
        try {
            int count = adminProductService.bulkRegister(csvFile);
            return ResponseEntity.ok(Map.of("registeredCount", count));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("일괄등록 실패: " + e.getMessage());
        }
    }

}
