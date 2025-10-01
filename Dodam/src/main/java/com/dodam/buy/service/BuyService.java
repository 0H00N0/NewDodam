package com.dodam.buy.service;

import com.dodam.buy.entity.BuyEntity;
import com.dodam.buy.dto.BuyDTO;
import com.dodam.buy.repository.BuyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BuyService {
    private final BuyRepository buyRepo;

    public BuyDTO get(Long buynum) {
        BuyEntity entity = buyRepo.findById(buynum)
            .orElseThrow(() -> new RuntimeException("Buy not found: " + buynum));
        return toDTO(entity);
    }

    public BuyDTO save(BuyDTO dto) {
        BuyEntity entity = BuyEntity.builder()
            .buynum(dto.getBuynum())
            .mnum(dto.getMnum())
            .pronum(dto.getPronum())
            .catenum(dto.getCatenum())
            .prosnum(dto.getProsnum())
            .build();
        entity = buyRepo.save(entity);
        return toDTO(entity);
    }

    private BuyDTO toDTO(BuyEntity entity) {
        return BuyDTO.builder()
            .buynum(entity.getBuynum())
            .mnum(entity.getMnum())
            .pronum(entity.getPronum())
            .catenum(entity.getCatenum())
            .prosnum(entity.getProsnum())
            .build();
    }
}