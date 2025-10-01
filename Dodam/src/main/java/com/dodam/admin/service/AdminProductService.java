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
import org.springframework.web.multipart.MultipartFile;

import com.opencsv.CSVReader;

import java.io.Reader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProstateRepository prostateRepository;

    /**
     * 상품 등록
     */
    @Transactional
    public ProductEntity createProduct(AdminProductRequestDTO requestDTO) {
        CategoryEntity category = categoryRepository.findById(requestDTO.getCatenum())
                .orElseThrow(() -> new EntityNotFoundException("카테고리를 찾을 수 없습니다. ID: " + requestDTO.getCatenum()));

        ProstateEntity prostate = prostateRepository.findById(requestDTO.getProsnum())
                .orElseThrow(() -> new EntityNotFoundException("상품 상태를 찾을 수 없습니다. ID: " + requestDTO.getProsnum()));

        ProductEntity newProduct = ProductEntity.builder()
                .proname(requestDTO.getProname())
                .prodetail(requestDTO.getProdetail())
                .proborrow(requestDTO.getProborrow())
                .probrand(requestDTO.getProbrand())
                .promade(requestDTO.getPromade())
                .proage(requestDTO.getProage())
                .procertif(requestDTO.getProcertif())
                .prodate(requestDTO.getProdate())
                .category(category)
                .prostate(prostate)
                .images(new ArrayList<>())
                .build();

        // ✅ DTO에서 URL 그대로 넣기
        if (requestDTO.getImages() != null) {
            for (AdminProductRequestDTO.ImageDTO imageDTO : requestDTO.getImages()) {
                ProductImageEntity productImage = ProductImageEntity.builder()
                        .proimageorder(imageDTO.getProimageorder())
                        .prourl(imageDTO.getProurl())
                        .prodetailimage(imageDTO.getProdetailimage())
                        .product(newProduct)
                        .category(category)
                        .build();
                newProduct.getImages().add(productImage);
            }
        }

        return productRepository.save(newProduct);
    }

    /**
     * 상품 수정
     */
    @Transactional
    public ProductDetailResponseDTO updateProduct(Long productId, AdminProductRequestDTO requestDTO) {
        ProductEntity productToUpdate = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("상품을 찾을 수 없습니다. ID: " + productId));

        CategoryEntity category = categoryRepository.findById(requestDTO.getCatenum())
                .orElseThrow(() -> new EntityNotFoundException("카테고리를 찾을 수 없습니다. ID: " + requestDTO.getCatenum()));

        ProstateEntity prostate = prostateRepository.findById(requestDTO.getProsnum())
                .orElseThrow(() -> new EntityNotFoundException("상품 상태를 찾을 수 없습니다. ID: " + requestDTO.getProsnum()));

        // 기본 정보 업데이트
        productToUpdate.setProname(requestDTO.getProname());
        productToUpdate.setProdetail(requestDTO.getProdetail());
        productToUpdate.setProborrow(requestDTO.getProborrow());
        productToUpdate.setProbrand(requestDTO.getProbrand());
        productToUpdate.setPromade(requestDTO.getPromade());
        productToUpdate.setProage(requestDTO.getProage());
        productToUpdate.setProcertif(requestDTO.getProcertif());
        productToUpdate.setProdate(requestDTO.getProdate());
        productToUpdate.setCategory(category);
        productToUpdate.setProstate(prostate);

        // ✅ 이미지 초기화 후 새 URL 적용
        productToUpdate.getImages().clear();
        if (requestDTO.getImages() != null) {
            for (AdminProductRequestDTO.ImageDTO imageDTO : requestDTO.getImages()) {
                ProductImageEntity newImage = ProductImageEntity.builder()
                        .proimageorder(imageDTO.getProimageorder())
                        .prourl(imageDTO.getProurl())
                        .prodetailimage(imageDTO.getProdetailimage())
                        .product(productToUpdate)
                        .category(category)
                        .build();
                productToUpdate.getImages().add(newImage);
            }
        }

        return new ProductDetailResponseDTO(productToUpdate);
    }

    /**
     * 상품 일괄 등록 (CSV → URL 직접 입력)
     */
    @Transactional
    public int bulkRegister(MultipartFile csvFile) throws Exception {
        List<ProductEntity> products = new ArrayList<>();
        try (Reader reader = new InputStreamReader(csvFile.getInputStream());
             CSVReader csvReader = new CSVReader(reader)) {

            String[] line;
            int row = 0;
            while ((line = csvReader.readNext()) != null) {
                if (row++ == 0) continue; // 헤더 스킵

                ProductEntity product = ProductEntity.builder()
                        .proname(line[0])                                // 상품명
                        .prodetail(line[1])                             // 상세설명
                        .proborrow(new BigDecimal(line[2]))              // 대여료
                        .probrand(line[3])                              // 브랜드
                        .promade(line[4])                               // 제조사
                        .proage(Integer.parseInt(line[5]))              // 연령
                        .procertif(line[6])                             // 인증
                        .prodate(LocalDate.parse(line[7]))              // 출시일
                        .category(categoryRepository.findById(Long.parseLong(line[8])).orElseThrow()) // 카테고리
                        .prostate(prostateRepository.findById(Long.parseLong(line[9])).orElseThrow()) // 상태
                        .images(new ArrayList<>())
                        .build();

                // ✅ 10번째(메인), 11번째(상세) 컬럼에서 이미지 URL 읽기
                if (line.length > 10 && line[10] != null && !line[10].isBlank()) {
                    product.getImages().add(ProductImageEntity.builder()
                            .proimageorder(1)
                            .prourl(line[10])
                            .product(product)
                            .category(product.getCategory())
                            .build());
                }
                if (line.length > 11 && line[11] != null && !line[11].isBlank()) {
                    product.getImages().add(ProductImageEntity.builder()
                            .proimageorder(2)
                            .prodetailimage(line[11])
                            .product(product)
                            .category(product.getCategory())
                            .build());
                }

                products.add(product);
            }
        }

        productRepository.saveAll(products);
        return products.size();
    }


    /**
     * 전체 상품 조회
     */
    @Transactional(readOnly = true)
    public List<ProductListResponseDTO> findAllProducts() {
        return productRepository.findAll().stream()
                .map(ProductListResponseDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * 상품 단일 조회
     */
    @Transactional(readOnly = true)
    public ProductDetailResponseDTO findProductById(Long productId) {
        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("상품을 찾을 수 없습니다. ID: " + productId));
        return new ProductDetailResponseDTO(product);
    }

    /**
     * 상품 삭제
     */
    @Transactional
    public void deleteProduct(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new EntityNotFoundException("상품을 찾을 수 없어 삭제할 수 없습니다. ID: " + productId);
        }
        productRepository.deleteById(productId);
    }
}