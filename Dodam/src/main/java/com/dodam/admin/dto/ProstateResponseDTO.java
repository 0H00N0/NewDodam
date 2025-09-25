package com.dodam.admin.dto;

import com.dodam.product.entity.ProstateEntity;
import lombok.Getter;

@Getter
public class ProstateResponseDTO {
    private Long prosnum;
    private String prograde;

    public ProstateResponseDTO(ProstateEntity prostate) {
        this.prosnum = prostate.getProsnum();
        this.prograde = prostate.getPrograde();
    }
}