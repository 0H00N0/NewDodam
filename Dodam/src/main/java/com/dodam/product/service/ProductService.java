package com.dodam.product.service;

import com.dodam.product.dto.*;
import com.dodam.product.entity.*;
import com.dodam.product.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository productRepo;
    private final CategoryRepository categoryRepo;
    private final ProstateRepository prostateRepo;
    private final ProductImageRepository imageRepo;

    // 이미지 절대 URL 기본값 (배포 시 env로 변경 권장)
    private final String baseImageUrl = "http://localhost:8080/images";

    // ====== 검색 ======
    @Transactional(readOnly = true)
    public Page<ProductDTO> searchByColumns(String q, Long catenum, Long prosnum, String prograde, Pageable pageable) {
        Specification<ProductEntity> spec = (root, cq, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim() + "%";
                preds.add(cb.or(
                    cb.like(root.get("proname"), like),
                    cb.like(root.get("probrand"), like)
                ));
            }
            if (catenum != null) {
                preds.add(cb.equal(root.get("category").get("catenum"), catenum));
            }
            if (prosnum != null) {
                preds.add(cb.equal(root.get("prostate").get("prosnum"), prosnum));
            }
            if (prograde != null && !prograde.isBlank()) {
                preds.add(cb.equal(root.get("prostate").get("prograde"), prograde.trim()));
            }
            return preds.isEmpty() ? null : cb.and(preds.toArray(Predicate[]::new));
        };

        // ✅ 각 상품 DTO에 대표 이미지(thumbnailUrl) 자동 주입
        return productRepo.findAll(spec, pageable)
                .map(p -> toDTO(p, true));
    }

    @Transactional(readOnly = true)
    public ProductDTO get(Long pronum) {
        ProductEntity p = productRepo.findById(pronum)
            .orElseThrow(() -> new NoSuchElementException("product not found: " + pronum));
        return toDTO(p, true);
    }

    // ====== 생성 ======
    public ProductDTO create(ProductDTO d) {
        CategoryEntity category = categoryRepo.findById(req(d.getCatenum(), "catenum"))
            .orElseThrow(() -> new NoSuchElementException("category not found: " + d.getCatenum()));
        ProstateEntity status = prostateRepo.findById(req(d.getProsnum(), "prosnum"))
            .orElseThrow(() -> new NoSuchElementException("prostate not found: " + d.getProsnum()));

        ProductEntity p = new ProductEntity();
        apply(d, p, category, status);
        p = productRepo.save(p);

        saveImages(p, d.getImages());
        return toDTO(p, true);
    }

    // ====== 수정 ======
    public ProductDTO update(ProductDTO d) {
        ProductEntity p = productRepo.findById(req(d.getPronum(), "pronum"))
            .orElseThrow(() -> new NoSuchElementException("product not found: " + d.getPronum()));

        CategoryEntity category = (d.getCatenum() != null)
            ? categoryRepo.findById(d.getCatenum()).orElseThrow(() -> new NoSuchElementException("category not found: " + d.getCatenum()))
            : p.getCategory();

        ProstateEntity status = (d.getProsnum() != null)
            ? prostateRepo.findById(d.getProsnum()).orElseThrow(() -> new NoSuchElementException("prostate not found: " + d.getProsnum()))
            : p.getProstate();

        applyPartial(d, p, category, status);

        if (d.getImages() != null) {
            imageRepo.deleteByProduct(p);
            saveImages(p, d.getImages());
        }
        return toDTO(p, true);
    }

    public void delete(Long pronum) {
        ProductEntity p = productRepo.findById(pronum)
            .orElseThrow(() -> new NoSuchElementException("product not found: " + pronum));
        imageRepo.deleteByProduct(p);
        productRepo.delete(p);
    }

    // ====== 매핑 ======
    private static Long req(Long v, String name) {
        if (v == null) throw new IllegalArgumentException(name + " is required");
        return v;
    }

    private void apply(ProductDTO d, ProductEntity p, CategoryEntity cat, ProstateEntity st) {
        p.setCategory(cat);
        p.setProstate(st);
        p.setProname(d.getProname());
        p.setProdetail(d.getProdetail());
        p.setProprice(d.getProprice());
        p.setProborrow(d.getProborrow());
        p.setProbrand(d.getProbrand());
        p.setPromade(d.getPromade());
        p.setProage(d.getProage());
        p.setProcertif(d.getProcertif());
        p.setProdate(d.getProdate());
    }

    private void applyPartial(ProductDTO d, ProductEntity p, CategoryEntity cat, ProstateEntity st) {
        p.setCategory(cat);
        p.setProstate(st);
        if (d.getProname()   != null) p.setProname(d.getProname());
        if (d.getProdetail() != null) p.setProdetail(d.getProdetail());
        if (d.getProprice()  != null) p.setProprice(d.getProprice());
        if (d.getProborrow() != null) p.setProborrow(d.getProborrow());
        if (d.getProbrand()  != null) p.setProbrand(d.getProbrand());
        if (d.getPromade()   != null) p.setPromade(d.getPromade());
        if (d.getProage()    != null) p.setProage(d.getProage());
        if (d.getProcertif() != null) p.setProcertif(d.getProcertif());
        if (d.getProdate()   != null) p.setProdate(d.getProdate());
    }

    private ProductDTO toDTO(ProductEntity p, boolean withImages) {
        ProductDTO.ProductDTOBuilder b = ProductDTO.builder()
            .pronum(p.getPronum())
            .proname(p.getProname())
            .prodetail(p.getProdetail())
            .proprice(p.getProprice())
            .proborrow(p.getProborrow())
            .probrand(p.getProbrand())
            .promade(p.getPromade())
            .proage(p.getProage())
            .procertif(p.getProcertif())
            .prodate(p.getProdate())
            .procre(p.getProcre())
            .proupdate(p.getProupdate())
            .catenum(p.getCategory() != null ? p.getCategory().getCatenum() : null)
            .prosnum(p.getProstate() != null ? p.getProstate().getProsnum() : null);

        List<ProductImageEntity> imgs = p.getImages();
        if (withImages && imgs != null && !imgs.isEmpty()) {
            List<ProductImageDTO> imgDtos = imgs.stream()
                    .map(e -> ProductImageDTO.builder()
                            .proimagenum(e.getProimagenum())
                            .proimageorder(e.getProimageorder())
                            .prourl(e.getProurl())
                            .prodetailimage(e.getProdetailimage())
                            .catenum(e.getCategory() != null ? e.getCategory().getCatenum() : null)
                            .pronum(p.getPronum())
                            .build())
                    .collect(Collectors.toList());
            b.images(imgDtos);

            // ✅ 첫 번째 이미지를 대표 이미지로 지정
            ProductImageEntity first = imgs.get(0);
            String raw = (first.getProurl() != null && !first.getProurl().isBlank())
                    ? first.getProurl()
                    : first.getProdetailimage();
            if (raw != null && !raw.isBlank()) {
                if (raw.startsWith("http") || raw.startsWith("data:")) {
                    b.thumbnailUrl(raw);
                } else {
                    b.thumbnailUrl(baseImageUrl + "/" + raw);
                }
            }
        }

        return b.build();
    }

    private void saveImages(ProductEntity p, List<ProductImageDTO> images) {
        if (images == null || images.isEmpty()) return;

        Map<Integer, ProductImageEntity> byOrder = new LinkedHashMap<>();
        for (ProductImageDTO im : images) {
            if (im.getProimageorder() == null) continue;

            ProductImageEntity row = byOrder.computeIfAbsent(im.getProimageorder(), k -> {
                ProductImageEntity e = new ProductImageEntity();
                e.setProduct(p);
                e.setCategory(p.getCategory());
                e.setProimageorder(k);
                return e;
            });

            if (im.getProurl() != null && !im.getProurl().isBlank()) {
                row.setProurl(im.getProurl());
            }
            if (im.getProdetailimage() != null && !im.getProdetailimage().isBlank()) {
                row.setProdetailimage(im.getProdetailimage());
            }
        }
        imageRepo.saveAll(byOrder.values());
    }

    // ===== 카테고리별 조회 =====
    @Transactional(readOnly = true)
    public List<ProductDTO> findByCategoryName(String categoryName) {
        CategoryEntity category = categoryRepo.findAll().stream()
            .filter(cat -> categoryName.equals(cat.getCatename()))
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException("category not found: " + categoryName));

        List<ProductEntity> products = productRepo.findByCategory(category);
        return products.stream()
                .map(p -> toDTO(p, true))
                .collect(Collectors.toList());
    }

    // ===== 추가: 상품 이미지 URL 목록 반환 =====
    @Transactional(readOnly = true)
    public List<String> getProductImageUrls(Long proId, Integer limit) {
        List<ProductImageEntity> imgs = imageRepo.findAll().stream()
            .filter(img -> img.getProduct() != null && Objects.equals(img.getProduct().getPronum(), proId))
            .sorted(Comparator.comparingInt(e -> e.getProimageorder() == null ? Integer.MAX_VALUE : e.getProimageorder()))
            .collect(Collectors.toList());

        if (imgs.isEmpty()) return Collections.emptyList();

        List<String> names = imgs.stream()
            .map(e -> {
                String prourl = e.getProurl();
                String detail = e.getProdetailimage();
                if (prourl != null && !prourl.isBlank()) return prourl.trim();
                if (detail != null && !detail.isBlank()) return detail.trim();
                return null;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        if (names.isEmpty()) return Collections.emptyList();

        final String TINY_GIF = "data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7";
        final int MIN_DATA_URI_LENGTH = 200;

        List<String> filtered = names.stream()
            .filter(n -> {
                if (n == null) return false;
                if (n.startsWith("data:")) {
                    if (n.equals(TINY_GIF)) return false;
                    return n.length() >= MIN_DATA_URI_LENGTH;
                }
                return true;
            })
            .collect(Collectors.toList());

        if (filtered.isEmpty()) return Collections.emptyList();

        if (limit != null && limit > 0 && limit < filtered.size()) {
            filtered = filtered.subList(0, limit);
        }

        return filtered.stream()
                .map(name -> {
                    if (name.startsWith("data:") || name.startsWith("http")) return name;
                    return baseImageUrl + "/" + name;
                })
                .collect(Collectors.toList());
    }
}
