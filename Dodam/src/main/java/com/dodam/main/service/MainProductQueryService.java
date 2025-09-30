package com.dodam.main.service;

import com.dodam.main.dto.MainNewProductByNameDTO;
import com.dodam.main.dto.MainPopularProductByNameDTO;
import com.dodam.main.dto.MainProductBasicDTO;
import com.dodam.main.repository.MainProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MainProductQueryService {

    private final MainProductRepository repo;

    public List<MainNewProductByNameDTO> getNewProductsByName(int limit) {
        return repo.findNewProductsByName(limit).stream()
            .map(r -> new MainNewProductByNameDTO(
                (String) r[0],   // NAME
                toLong(r[1]),    // PROID (PRONUM)
                (String) r[2],   // IMAGE_URL
                toLong(r[3]),    // PRICE (PROBORROW)
                (String) r[4]    // PROCRE (문자열)
            ))
            .collect(Collectors.toList());
    }

    public List<MainPopularProductByNameDTO> getPopularProductsByName(int limit) {
        return repo.findPopularProductsByName(limit).stream()
            .map(r -> new MainPopularProductByNameDTO(
                (String) r[0],   // NAME
                toLong(r[1]),    // PROID
                (String) r[2],   // IMAGE_URL
                toLong(r[3]),    // PRICE
                toLong(r[4])     // RENTCOUNT
            ))
            .collect(Collectors.toList());
    }

    /** ✅ 단건 상세 (히어로 onPrimary로 상세 이동 시 사용) */
    public MainProductBasicDTO getProductBasic(Long proId) {
        Object[] r = repo.findProductBasicById(proId);
        if (r == null) {
            throw new IllegalArgumentException("Product not found: " + proId);
        }
        return new MainProductBasicDTO(
            toLong(r[0]),        // PROID
            (String) r[1],       // NAME
            toLong(r[2]),        // PRICE
            (String) r[3],       // PROCRE
            (String) r[4]        // IMAGE_URL
        );
    }
    
    /** ✅ 해당 상품 이미지 URL 리스트 */
    public List<String> getProductImageUrls(Long proId, Integer limit) {
        if (proId == null) return List.of();
        if (limit == null || limit <= 0) {
            return repo.findAllImageUrlsByProId(proId);
        }
        return repo.findImageUrlsByProIdLimited(proId, limit);
    }

    private Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Long l) return l;
        if (o instanceof Integer i) return (long) i;
        if (o instanceof BigInteger bi) return bi.longValue();
        if (o instanceof BigDecimal bd) return bd.longValue();
        return Long.parseLong(o.toString());
    }
}
