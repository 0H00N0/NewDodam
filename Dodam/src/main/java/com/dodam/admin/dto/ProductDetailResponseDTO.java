package com.dodam.admin.dto;

import com.dodam.product.entity.ProductEntity;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class ProductDetailResponseDTO { //상품 상세 조회 dto

    private Long pronum;
    private String proname;
    private String prodetail;
    private BigDecimal proprice;
    private BigDecimal proborrow;
    private String probrand;
    private String promade;
    private Integer proage;
    private String procertif;
    private LocalDate prodate;
    private LocalDateTime procre;
    private LocalDateTime proupdate;

    // 연관 정보
    private String categoryName;
    private String productGrade;

    // 이미지 정보
    private List<ImageInfoDTO> images;

    public ProductDetailResponseDTO(ProductEntity product) {
        this.pronum = product.getPronum();
        this.proname = product.getProname();
        this.prodetail = product.getProdetail();
        this.proprice = product.getProprice();
        this.proborrow = product.getProborrow();
        this.probrand = product.getProbrand();
        this.promade = product.getPromade();
        this.proage = product.getProage();
        this.procertif = product.getProcertif();
        this.prodate = product.getProdate();
        this.procre = product.getProcre();
        this.proupdate = product.getProupdate();

        if (product.getCategory() != null) {
            this.categoryName = product.getCategory().getCatename();
        }
        if (product.getProstate() != null) {
            this.productGrade = product.getProstate().getPrograde();
        }

        if (product.getImages() != null) {
            this.images = product.getImages().stream()
                    .map(ImageInfoDTO::new)
                    .collect(Collectors.toList());
        }
    }

    @Getter
    private static class ImageInfoDTO {
        private Long proimagenum;
        private Integer proimageorder;
        private String prourl;
        private String prodetailimage;

        public ImageInfoDTO(com.dodam.product.entity.ProductImageEntity image) {
            this.proimagenum = image.getProimagenum();
            this.proimageorder = image.getProimageorder();
            this.prourl = image.getProurl();
            this.prodetailimage = image.getProdetailimage();
        }
    }
}