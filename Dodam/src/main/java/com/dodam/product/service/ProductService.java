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

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository productRepo;
    private final CategoryRepository categoryRepo;
    private final ProstateRepository prostateRepo;
    private final ProductImageRepository imageRepo;

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
        return productRepo.findAll(spec, pageable).map(p -> toDTO(p, true));
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
        p.setResernum(d.getResernum());
        p.setCtnum(d.getCtnum());
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
        if (d.getResernum()  != null) p.setResernum(d.getResernum());
        if (d.getCtnum()     != null) p.setCtnum(d.getCtnum());
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
            .prosnum(p.getProstate() != null ? p.getProstate().getProsnum() : null)
            .resernum(p.getResernum())
            .ctnum(p.getCtnum());

        if (withImages && p.getImages() != null) {
            List<ProductImageDTO> imgs = new ArrayList<>();
            for (ProductImageEntity e : p.getImages()) {
                imgs.add(ProductImageDTO.builder()
                    .proimagenum(e.getProimagenum())
                    .proimageorder(e.getProimageorder())
                    .prourl(e.getProurl())
                    .prodetailimage(e.getProdetailimage())
                    .catenum(e.getCategory() != null ? e.getCategory().getCatenum() : null)
                    .pronum(p.getPronum())
                    .build());
            }
            b.images(imgs);
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
                e.setCategory(p.getCategory()); // productimage.catenum = product.catenum
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
}
