package com.dodam.admin.dto;

import com.dodam.delivery.entity.DeliverymanEntity;
import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;

@Getter
@Builder
public class DeliverymanResponseDTO {
    private Long delnum;
    private Long pronum;
    private String proname;
    private Long mnum;
    private String mname;
    private Integer dayoff;
    private BigDecimal delcost;
    private String location;

    public static DeliverymanResponseDTO fromEntity(DeliverymanEntity e) {
        return DeliverymanResponseDTO.builder()
                .delnum(e.getDelnum())
                .pronum(e.getProduct() != null ? e.getProduct().getPronum() : null)
                .proname(e.getProduct() != null ? e.getProduct().getProname() : null)
                .mnum(e.getMember() != null ? e.getMember().getMnum() : null)
                .mname(e.getMember() != null ? e.getMember().getMname() : null)
                .dayoff(e.getDayoff())
                .delcost(e.getDelcost())
                .location(e.getLocation())
                .build();
    }
}
