package com.dodam.admin.dto;

import com.dodam.plan.Entity.PlanBenefitEntity;
import com.dodam.plan.Entity.PlanInvoiceEntity;
import com.dodam.plan.Entity.PlanMember;
import com.dodam.plan.Entity.PlanNameEntity;
import com.dodam.plan.Entity.PlanPriceEntity; // PlanPriceEntity import 추가
import com.dodam.plan.Entity.PlansEntity;
import com.dodam.plan.Entity.PlanTermsEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal; // BigDecimal import 추가
import java.time.LocalDateTime;
import java.util.List; // List import 추가
import java.util.stream.Collectors; // Collectors import 추가


public class AdminPlanDto {

    // =================== 코드 수정/추가된 부분 시작 ===================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanPriceDto {
        private Long ppriceId;
        private Integer termMonth; // PlanTermsEntity의 termMonth 필드를 직접 사용
        private String ppriceBilMode;
        private BigDecimal ppriceAmount;
        private String ppriceCurr;
        private Boolean ppriceActive;

        public static PlanPriceDto fromEntity(PlanPriceEntity entity) {
            return PlanPriceDto.builder()
                    .ppriceId(entity.getPpriceId())
                    .termMonth(entity.getPterm().getPtermMonth()) // PlanTermsEntity에서 기간(개월) 가져오기
                    .ppriceBilMode(entity.getPpriceBilMode())
                    .ppriceAmount(entity.getPpriceAmount())
                    .ppriceCurr(entity.getPpriceCurr())
                    .ppriceActive(entity.getPpriceActive())
                    .build();
        }
    }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long planId;
        private String planName;
        private String planCode;
        private Boolean planActive;
        private LocalDateTime planCreate;
        private List<PlanPriceDto> prices; // 가격 정보 리스트 필드 추가
        private List<PlanBenefitDto> benefits; // ▼ 혜택 리스트 필드 추가

        // fromEntity 메서드는 이제 PlansEntity와 PlanPriceEntity 리스트를 모두 필요로 합니다.
        // 이 로직은 Service 레이어에서 처리하는 것이 더 적합합니다.
        // 아래는 Service에서 DTO를 조립하는 예시 코드의 일부입니다.
        /*
            public static Response fromEntities(PlansEntity plansEntity, List<PlanPriceEntity> priceEntities) {
                List<PlanPriceDto> priceDtos = priceEntities.stream()
                        .map(PlanPriceDto::fromEntity)
                        .collect(Collectors.toList());

                return Response.builder()
                        .planId(plansEntity.getPlanId())
                        .planName(plansEntity.getPlanName().getPlanName())
                        .planCode(plansEntity.getPlanCode())
                        .planActive(plansEntity.getPlanActive())
                        .planCreate(plansEntity.getPlanCreate())
                        .prices(priceDtos) // 조립된 가격 DTO 리스트를 설정
                        .build();
            }
        */
    }
 // ▼ 혜택 정보 DTO 내부 클래스 추가
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PlanBenefitDto {
        private BigDecimal pbPriceCap;
        private String pbNote;

        public static PlanBenefitDto fromEntity(PlanBenefitEntity entity) {
            return PlanBenefitDto.builder()
                    .pbPriceCap(entity.getPbPriceCap())
                    .pbNote(entity.getPbNote())
                    .build();
        }
    }

    // =================== 코드 수정/추가된 부분 끝 =====================


 // =================== ▼ 수정된 부분 시작 ▼ ===================
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CreateRequest {
        // 기존 planNameId 대신 planName (텍스트)을 받도록 변경
        private String planName;
        private String planCode;
        private Boolean planActive;
        // 가격 정보 리스트를 받기 위한 필드 추가
        private List<PlanPriceDto> prices;
        private List<PlanBenefitDto> benefits; // ▼ 혜택 리스트 필드 추가
        // toEntity 메서드는 서비스 레이어에서 처리하므로 여기서는 제거하거나 주석처리 합니다.
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UpdateRequest {
        // 기존 planNameId 대신 planName (텍스트)을 받도록 변경
        private String planName;
        private Boolean planActive;
        // 가격 정보 리스트를 받기 위한 필드 추가
        private List<PlanPriceDto> prices;
        private List<PlanBenefitDto> benefits; // ▼ 혜택 리스트 필드 추가
    }
    // =================== ▲ 수정된 부분 끝 ▲ ===================
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public class AdminPlanDetailDto {
        private Long planId;
        private String planName;
        private String planCode;
        private int price;
        private String term;     // 1개월, 3개월, 12개월 등
        private List<String> benefits; // 혜택 리스트
    }
 // =================== ▼ PlanMember DTO 추가 ▼ ===================
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanMemberDto {
        private Long pmId;
        private Long memberId;
        private String memberName;
        private String planName;
        private String pmStat;
        private String pmBilMode;
        private LocalDateTime pmStart;
        private LocalDateTime pmTermStart;
        private LocalDateTime pmTermEnd;
        private LocalDateTime pmNextBil;
        private Integer pmCycle;
        private boolean pmCancelCheck;

        public static PlanMemberDto fromEntity(PlanMember entity) {
            return PlanMemberDto.builder()
                    .pmId(entity.getPmId())
                    .memberId(entity.getMember().getMnum())
                    .memberName(entity.getMember().getMname())
                    .planName(entity.getPlan().getPlanName().getPlanName()) // 🔥 String 필드로 접근
                    .pmStat(entity.getPmStat().name())
                    .pmBilMode(entity.getPmBilMode().name())
                    .pmStart(entity.getPmStart())
                    .pmTermStart(entity.getPmTermStart())
                    .pmTermEnd(entity.getPmTermEnd())
                    .pmNextBil(entity.getPmNextBil())
                    .pmCycle(entity.getPmCycle())
                    .pmCancelCheck(entity.isPmCancelCheck())
                    .build();
        }


    }
    // =================== ▲ PlanMember DTO 추가 끝 ▲ ===================


    // =================== ▼ PlanInvoice DTO 추가 ▼ ===================
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanInvoiceDto {
        private Long piId;
        private Long pmId;
        private Long memberId;
        private String memberName;
        private String planName;
        private BigDecimal piAmount;
        private String piCurr;
        private String piStat;
        private String piUid;
        private LocalDateTime piStart;
        private LocalDateTime piEnd;
        private LocalDateTime piPaid;

        public static PlanInvoiceDto fromEntity(PlanInvoiceEntity entity) {
            return PlanInvoiceDto.builder()
                    .piId(entity.getPiId())
                    .pmId(entity.getPlanMember().getPmId())
                    .memberId(entity.getPlanMember().getMember().getMnum())
                    .memberName(entity.getPlanMember().getMember().getMname())
                    .planName(entity.getPlanMember().getPlan().getPlanName().getPlanName()) // 🔥 PlanNameEntity → String
                    .piAmount(entity.getPiAmount())
                    .piCurr(entity.getPiCurr())
                    .piStat(entity.getPiStat().name())
                    .piUid(entity.getPiUid())
                    .piStart(entity.getPiStart())
                    .piEnd(entity.getPiEnd())
                    .piPaid(entity.getPiPaid())
                    .build();
        }

    }
    // =================== ▲ PlanInvoice DTO 추가 끝 ▲ ===================

}