package com.dodam.admin.service;

import com.dodam.admin.dto.AdminProductRequestDTO;
import com.dodam.admin.dto.ProductDetailResponseDTO;
import com.dodam.admin.dto.ProductListResponseDTO;
import com.dodam.product.entity.CategoryEntity;
import com.dodam.product.entity.ProductEntity;
import com.dodam.product.entity.ProductImageEntity;
import com.dodam.product.entity.ProstateEntity;
import com.dodam.product.repository.CategoryRepository;
import com.dodam.product.repository.ProductRepository;
import com.dodam.product.repository.ProstateRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProstateRepository prostateRepository;

    @Transactional
    public ProductEntity createProduct(AdminProductRequestDTO requestDTO) {
        // 1. 연관 엔티티 조회
        CategoryEntity category = categoryRepository.findById(requestDTO.getCatenum())
                .orElseThrow(() -> new EntityNotFoundException("해당 카테고리를 찾을 수 없습니다. ID: " + requestDTO.getCatenum()));

        ProstateEntity prostate = prostateRepository.findById(requestDTO.getProsnum())
                .orElseThrow(() -> new EntityNotFoundException("해당 상품 상태를 찾을 수 없습니다. ID: " + requestDTO.getProsnum()));

        // 2. DTO -> Entity 변환 (모든 필드 포함)
        ProductEntity newProduct = ProductEntity.builder()
                .proname(requestDTO.getProname()) // 👈 이 부분이 누락되었을 가능성이 매우 높습니다.
                .prodetail(requestDTO.getProdetail())
                .proprice(requestDTO.getProprice())
                .proborrow(requestDTO.getProborrow())
                .probrand(requestDTO.getProbrand())
                .promade(requestDTO.getPromade())
                .proage(requestDTO.getProage())
                .procertif(requestDTO.getProcertif())
                .prodate(requestDTO.getProdate())
                .resernum(requestDTO.getResernum())
                .ctnum(requestDTO.getCtnum())
                .category(category)
                .prostate(prostate)
                .images(new ArrayList<>())
                .build();

        // 3. 이미지 정보 처리
        if (requestDTO.getImageName() != null && !requestDTO.getImageName().isEmpty()) {
            ProductImageEntity productImage = ProductImageEntity.builder()
                    .proimageorder(1)
                    .prourl(requestDTO.getImageName())
                    .prodetailimage(requestDTO.getImageName())
                    .product(newProduct)
                    .category(category)
                    .build();
            newProduct.getImages().add(productImage);
        }

        // 4. DB에 저장
        return productRepository.save(newProduct);
        
    }
    /**
     * 모든 상품 목록을 조회하여 DTO 리스트로 반환합니다.
     * @return 상품 목록 DTO 리스트
     */
    @Transactional(readOnly = true)
    public List<ProductListResponseDTO> findAllProducts() {
        return productRepository.findAll().stream()
                .map(ProductListResponseDTO::new)
                .collect(Collectors.toList());
    }
    /**
     * ID로 특정 상품의 상세 정보를 조회합니다.
     * @param productId 조회할 상품의 ID
     * @return 상품 상세 정보 DTO
     */
    @Transactional(readOnly = true)
    public ProductDetailResponseDTO findProductById(Long productId) {
        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("해당 ID의 상품을 찾을 수 없습니다: " + productId));
        return new ProductDetailResponseDTO(product);
    }
    /**
     * 상품 정보를 수정합니다.
     * @param productId 수정할 상품의 ID
     * @param requestDTO 수정할 상품 정보
     * @return 수정된 상품 상세 정보 DTO
     */
    @Transactional
    public ProductDetailResponseDTO updateProduct(Long productId, AdminProductRequestDTO requestDTO) {
        // 1. 기존 상품 엔티티 조회
        ProductEntity productToUpdate = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("해당 ID의 상품을 찾을 수 없습니다: " + productId));

        // 2. 연관 엔티티 조회 (카테고리, 상품 상태)
        CategoryEntity category = categoryRepository.findById(requestDTO.getCatenum())
                .orElseThrow(() -> new EntityNotFoundException("해당 카테고리를 찾을 수 없습니다. ID: " + requestDTO.getCatenum()));

        ProstateEntity prostate = prostateRepository.findById(requestDTO.getProsnum())
                .orElseThrow(() -> new EntityNotFoundException("해당 상품 상태를 찾을 수 없습니다. ID: " + requestDTO.getProsnum()));

        // 3. DTO의 내용으로 엔티티의 필드 값 변경 (Setter 사용)
        productToUpdate.setProname(requestDTO.getProname());
        productToUpdate.setProdetail(requestDTO.getProdetail());
        productToUpdate.setProprice(requestDTO.getProprice());
        productToUpdate.setProborrow(requestDTO.getProborrow());
        productToUpdate.setProbrand(requestDTO.getProbrand());
        productToUpdate.setPromade(requestDTO.getPromade());
        productToUpdate.setProage(requestDTO.getProage());
        productToUpdate.setProcertif(requestDTO.getProcertif());
        productToUpdate.setProdate(requestDTO.getProdate());
        productToUpdate.setResernum(requestDTO.getResernum());
        productToUpdate.setCtnum(requestDTO.getCtnum());
        productToUpdate.setCategory(category);
        productToUpdate.setProstate(prostate);

        // 4. 이미지 정보 업데이트 (기존 이미지 삭제 후 새로 추가)
        // ProductEntity의 @OneToMany에 orphanRemoval = true 설정 덕분에 리스트에서 제거하면 DB에서도 삭제됩니다.
        productToUpdate.getImages().clear();
        
        if (requestDTO.getImageName() != null && !requestDTO.getImageName().isEmpty()) {
            ProductImageEntity newImage = ProductImageEntity.builder()
                    .proimageorder(1)
                    .prourl(requestDTO.getImageName())
                    .prodetailimage(requestDTO.getImageName())
                    .product(productToUpdate) // 수정 대상인 상품과 연결
                    .category(category)
                    .build();
            productToUpdate.getImages().add(newImage);
        }
        
        // 5. 변경된 엔티티를 DTO로 변환하여 반환
        // @Transactional에 의해 메서드가 종료될 때 변경된 내용(Dirty Checking)이 자동으로 DB에 반영됩니다.
        // 따라서 productRepository.save()를 명시적으로 호출할 필요가 없습니다.
        return new ProductDetailResponseDTO(productToUpdate);
    }
    /**
     * ID로 상품을 삭제합니다.
     * @param productId 삭제할 상품의 ID
     */
    @Transactional
    public void deleteProduct(Long productId) {
        // 1. 삭제하려는 상품이 존재하는지 먼저 확인합니다.
        if (!productRepository.existsById(productId)) {
            throw new EntityNotFoundException("해당 ID의 상품을 찾을 수 없어 삭제할 수 없습니다: " + productId);
        }

        // 2. 상품을 삭제합니다.
        // ProductEntity와 ProductImageEntity가 (cascade = CascadeType.ALL, orphanRemoval = true)로
        // 설정되어 있으므로, 상품만 삭제해도 연관된 이미지들이 자동으로 함께 삭제됩니다.
        productRepository.deleteById(productId);
    }
    
}