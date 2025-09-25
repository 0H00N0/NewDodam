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

import java.util.List;
import java.util.Map;

@RestController 
@RequestMapping("/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final AdminProductService adminProductService;

    /**
     * 관리자 상품 등록 API
     * @param requestDTO 상품 정보와 이미지 정보를 담은 DTO
     * @return 생성된 상품의 ID
     */
    @PostMapping
    public ResponseEntity<?> registerProduct(@Valid @RequestBody AdminProductRequestDTO requestDTO) {
        ProductEntity createdProduct = adminProductService.createProduct(requestDTO);
        // 성공적으로 생성되었음을 알리는 201 Created 상태 코드와 함께,
        // 생성된 상품의 고유 ID(pronum)를 응답 body에 담아 반환합니다.
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("pronum", createdProduct.getPronum()));
    }
    @GetMapping
    public ResponseEntity<List<ProductListResponseDTO>> getAllProducts() {
        List<ProductListResponseDTO> products = adminProductService.findAllProducts();
        return ResponseEntity.ok(products);
    }
    // READ (Single): 특정 상품 상세 조회
    @GetMapping("/{productId}")
    public ResponseEntity<ProductDetailResponseDTO> getProductById(@PathVariable("productId") Long productId) {
        ProductDetailResponseDTO product = adminProductService.findProductById(productId);
        return ResponseEntity.ok(product);
    }
    // ⬆️ ====================================== ⬆️
    /**
     * 관리자 상품 수정 API
     * @param productId 수정할 상품의 ID
     * @param requestDTO 수정할 상품 정보 DTO
     * @return 수정된 상품 상세 정보
     */
    @PutMapping("/{productId}")
    public ResponseEntity<ProductDetailResponseDTO> updateProduct(
            @PathVariable("productId") Long productId,
            @Valid @RequestBody AdminProductRequestDTO requestDTO) {
        
        ProductDetailResponseDTO updatedProduct = adminProductService.updateProduct(productId, requestDTO);
        return ResponseEntity.ok(updatedProduct);
    }
    /**
     * 관리자 상품 삭제 API
     * @param productId 삭제할 상품의 ID
     * @return 성공 시 204 No Content
     */
    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(@PathVariable("productId") Long productId) {
        adminProductService.deleteProduct(productId);
        // 성공적으로 삭제되었으며, 별도의 본문(body)이 없음을 의미하는 204 No Content 상태를 반환합니다.
        return ResponseEntity.noContent().build();
    }
}