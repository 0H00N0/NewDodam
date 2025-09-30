package com.dodam.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class AdminProductRequestDTO { // 상품 등록/수정 DTO

    @NotBlank(message = "상품명은 필수 입력 항목입니다.")
    @Size(max = 200, message = "상품명은 최대 200자까지 입력 가능합니다.")
    private String proname;

    @Size(max = 2000, message = "상품 상세 설명은 최대 2000자까지 입력 가능합니다.")
    private String prodetail;

    private BigDecimal proprice;

    @NotNull(message = "대여 가격은 필수 입력 항목입니다.")
    @PositiveOrZero(message = "대여 가격은 0 이상이어야 합니다.")
    private BigDecimal proborrow;

    @NotBlank(message = "브랜드는 필수 입력 항목입니다.")
    private String probrand;

    private String promade;
    private Integer proage;
    private String procertif;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate prodate;

    @NotNull(message = "대여 예약 번호는 필수입니다.")
    private Long resernum;

    @NotNull(message = "카테고리 번호는 필수입니다.")
    private Long catenum;

    @NotNull(message = "상품 상태 번호는 필수입니다.")
    private Long prosnum;

    // ✅ 여러 이미지 URL을 받기 위한 리스트
    private List<ImageDTO> images;

    @Getter
    @Setter
    public static class ImageDTO {
        private Integer proimageorder;

        @NotBlank(message = "미리보기 이미지 URL은 필수입니다.")
        private String prourl;

        @NotBlank(message = "상세보기 이미지 URL은 필수입니다.")
        private String prodetailimage;
    }
}
