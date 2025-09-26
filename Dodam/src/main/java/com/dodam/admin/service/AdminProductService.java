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

import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.io.Reader;
import java.io.InputStreamReader;
import com.opencsv.CSVReader;


@Service
@RequiredArgsConstructor
public class AdminProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProstateRepository prostateRepository;
    private final FileUploadService fileUploadService; // 👈 추가

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
    /**
     * ✅ CSV + 이미지 파일을 이용한 상품 일괄등록
     */
    @Transactional
    public int bulkRegister(MultipartFile csvFile, MultipartFile[] images) throws Exception {
        // 1. 업로드된 이미지 파일 저장 (원본명 → 서버 저장명 매핑)
        Map<String, String> imagePathMap = saveImages(images);

        // 2. CSV 파싱
        List<ProductEntity> products = new ArrayList<>();
        try (Reader reader = new InputStreamReader(csvFile.getInputStream());
             CSVReader csvReader = new CSVReader(reader)) {

            String[] line;
            int row = 0;
            while ((line = csvReader.readNext()) != null) {
                if (row++ == 0) continue; // 헤더 스킵

                // CSV → 상품 엔티티 변환
                ProductEntity product = ProductEntity.builder()
                        .proname(line[0])                                // 상품명
                        .prodetail(line[1])                             // 상세설명
                        .proprice(new BigDecimal(line[2]))              // 가격
                        .proborrow(new BigDecimal(line[3]))             // 대여료
                        .probrand(line[4])                              // 브랜드
                        .promade(line[5])                               // 제조사
                        .proage(Integer.parseInt(line[6]))              // 연령
                        .procertif(line[7])                             // 인증
                        .prodate(LocalDate.parse(line[8]))              // 출시일
                        .resernum(Long.parseLong(line[9]))              // 예약번호
                        .ctnum(Long.parseLong(line[10]))                // ctnum
                        .category(categoryRepository.findById(Long.parseLong(line[11])).orElseThrow()) // 카테고리 FK
                        .prostate(prostateRepository.findById(Long.parseLong(line[12])).orElseThrow()) // 상태 FK
                        .images(new ArrayList<>())
                        .build();

                // 이미지 매핑 (메인/상세 이미지)
                if (line.length > 13 && imagePathMap.containsKey(line[13])) {
                    product.getImages().add(ProductImageEntity.builder()
                            .prourl(imagePathMap.get(line[13])) // 메인 이미지 경로
                            .proimageorder(1)
                            .product(product)
                            .category(product.getCategory())
                            .build());
                }
                if (line.length > 14 && imagePathMap.containsKey(line[14])) {
                    product.getImages().add(ProductImageEntity.builder()
                            .prodetailimage(imagePathMap.get(line[14])) // 상세 이미지 경로
                            .proimageorder(2)
                            .product(product)
                            .category(product.getCategory())
                            .build());
                }

                products.add(product);
                System.out.println("CSV 이미지명: " + line[13]);

                System.out.println("업로드된 파일명 map keys: " + imagePathMap.keySet());
            }
        }
        
        // 3. Bulk Insert
        productRepository.saveAll(products);
        return products.size();
    }

    /**
     * 이미지 파일 저장 후 (원본명 → 서버 저장명) 매핑 리턴
     */
    private Map<String, String> saveImages(MultipartFile[] images) throws IOException {
        Map<String, String> map = new HashMap<>();
        for (MultipartFile file : images) {
            String storedFileName = fileUploadService.storeFile(file); // UUID_파일명 반환
            map.put(file.getOriginalFilename(), storedFileName);
        }
        
        return map;
    }
}