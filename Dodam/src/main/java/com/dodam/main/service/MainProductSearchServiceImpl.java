// src/main/java/com/dodam/main/service/MainProductSearchServiceImpl.java
package com.dodam.main.service;

import com.dodam.main.dto.MainProductSearchDTO;
import com.dodam.main.repository.MainProductSearchRepository;
import com.dodam.main.repository.MainProductSearchSpecs;
import com.dodam.product.entity.ProductEntity;
import com.dodam.product.entity.ProductImageEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class MainProductSearchServiceImpl implements MainProductSearchService {

    private final MainProductSearchRepository productRepository;

    @Override
    public Page<MainProductSearchDTO> search(
            String q,
            Integer ageMin, Integer ageMax,
            BigDecimal priceMin, BigDecimal priceMax,
            Pageable pageable
    ) {
        Specification<ProductEntity> spec = Specification
                .where(MainProductSearchSpecs.nameContains(q))
                .and(MainProductSearchSpecs.ageBetween(ageMin, ageMax))
                .and(MainProductSearchSpecs.priceBetween(priceMin, priceMax));

        return productRepository.findAll(spec, pageable)
                .map(p -> new MainProductSearchDTO(
                        p.getPronum(),
                        p.getProname(),
                        p.getProborrow(),
                        firstThumb(p),
                        p.getProcre()
                ));
    }

    private String firstThumb(ProductEntity p) {
        if (p.getImages() == null || p.getImages().isEmpty()) return null;
        return p.getImages().stream()
                .sorted(Comparator.comparing(ProductImageEntity::getProimageorder,
                        Comparator.nullsLast(Integer::compareTo)))
                .map(ProductImageEntity::getProurl)
                .filter(u -> u != null && !u.isBlank())
                .findFirst().orElse(null);
    }
}
