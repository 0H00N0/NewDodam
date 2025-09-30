package com.dodam.admin.service;

import com.dodam.admin.dto.AdminPlanDto;
import com.dodam.admin.dto.RefundRequestDTO;
import com.dodam.plan.Entity.PlanAttemptEntity;
import com.dodam.plan.Entity.PlanBenefitEntity;
import com.dodam.plan.Entity.PlanInvoiceEntity;
import com.dodam.plan.Entity.PlanMember;
import com.dodam.plan.Entity.PlanNameEntity;
import com.dodam.plan.Entity.PlanRefundEntity;
import com.dodam.plan.Entity.PlanTermsEntity;
import com.dodam.plan.Entity.PlanPriceEntity;
import com.dodam.plan.Entity.PlansEntity;            // ✅ 플랜 기본정보 엔티티
import com.dodam.plan.enums.PlanEnums;
import com.dodam.plan.repository.PlanAttemptRepository;
import com.dodam.plan.repository.PlanBenefitRepository;
import com.dodam.plan.repository.PlanInvoiceRepository;
import com.dodam.plan.repository.PlanMemberRepository;
import com.dodam.plan.repository.PlanNameRepository;
import com.dodam.plan.repository.PlanPriceRepository;
import com.dodam.plan.repository.PlanRefundRepository;
import com.dodam.plan.repository.PlanTermsRepository;
import com.dodam.plan.repository.PlansRepository;    // ✅ PlansEntity용 리포지토리

import static com.dodam.plan.enums.PlanEnums.PrefMethod;
import static com.dodam.plan.enums.PlanEnums.PrefStatus;
import static com.dodam.plan.enums.PlanEnums.PrefType;


import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminPlanService {

    // -------------------------------
    // Repositories
    // -------------------------------
	private final PlansRepository plansRepository;
    private final PlanNameRepository planNameRepository;
    private final PlanPriceRepository planPriceRepository;
    private final PlanBenefitRepository planBenefitRepository;

    private final PlanMemberRepository planMemberRepository;
    private final PlanInvoiceRepository planInvoiceRepository;
    private final PlanAttemptRepository planAttemptRepository;
    private final PlanRefundRepository planRefundRepository;
    private final PlanTermsRepository planTermsRepository;

 // -------------------------------
    // ✅ Plan CRUD
    // -------------------------------

    public AdminPlanDto.Response createPlan(AdminPlanDto.CreateRequest requestDto) {
        // 1. 플랜 이름 처리 (없으면 신규 생성)
        PlanNameEntity planName = planNameRepository.findByPlanName(requestDto.getPlanName())
                .orElseGet(() -> planNameRepository.save(
                        PlanNameEntity.builder().planName(requestDto.getPlanName()).build()
                ));

        // 2. PlansEntity 생성
        PlansEntity plan = PlansEntity.builder()
                .planName(planName)
                .planCode(requestDto.getPlanCode())
                .planActive(requestDto.getPlanActive() != null ? requestDto.getPlanActive() : true)
                .build();
        PlansEntity savedPlan = plansRepository.save(plan);

        // 3. 가격 정보 저장
        if (requestDto.getPrices() != null) {
            for (AdminPlanDto.PlanPriceDto dto : requestDto.getPrices()) {
                PlanTermsEntity term = planTermsRepository.findByPtermMonth(dto.getTermMonth())
                        .orElseThrow(() -> new EntityNotFoundException("기간 정보를 찾을 수 없습니다."));

                PlanPriceEntity price = PlanPriceEntity.builder()
                        .plan(savedPlan)
                        .pterm(term)
                        .ppriceBilMode(dto.getPpriceBilMode())
                        .ppriceAmount(dto.getPpriceAmount())
                        .ppriceCurr(dto.getPpriceCurr())
                        .ppriceActive(dto.getPpriceActive())
                        .build();
                planPriceRepository.save(price);
            }
        }

        // 4. 혜택 정보 저장
        if (requestDto.getBenefits() != null) {
            for (AdminPlanDto.PlanBenefitDto dto : requestDto.getBenefits()) {
                PlanBenefitEntity benefit = PlanBenefitEntity.builder()
                        .plan(savedPlan)
                        .pbPriceCap(dto.getPbPriceCap())
                        .pbNote(dto.getPbNote())
                        .build();
                planBenefitRepository.save(benefit);
            }
        }

        return buildPlanResponse(savedPlan);
    }

    public List<AdminPlanDto.Response> getAllPlans() {
        return plansRepository.findAll()
                .stream()
                .map(this::buildPlanResponse)
                .collect(Collectors.toList());
    }

    public AdminPlanDto.Response getPlan(Long planId) {
        PlansEntity plan = plansRepository.findById(planId)
                .orElseThrow(() -> new EntityNotFoundException("Plan not found with id " + planId));
        return buildPlanResponse(plan);
    }

    public AdminPlanDto.Response updatePlan(Long planId, AdminPlanDto.UpdateRequest requestDto) {
        PlansEntity plan = plansRepository.findById(planId)
                .orElseThrow(() -> new EntityNotFoundException("Plan not found with id " + planId));

        // 이름 업데이트
        if (requestDto.getPlanName() != null) {
            PlanNameEntity planName = planNameRepository.findByPlanName(requestDto.getPlanName())
                    .orElseGet(() -> planNameRepository.save(
                            PlanNameEntity.builder().planName(requestDto.getPlanName()).build()
                    ));
            plan.setPlanName(planName);
        }

        // 상태 업데이트
        if (requestDto.getPlanActive() != null) {
            plan.setPlanActive(requestDto.getPlanActive());
        }

        PlansEntity updatedPlan = plansRepository.save(plan);

        // 가격 정보 갱신 (단순화: 기존 삭제 후 재저장)
        if (requestDto.getPrices() != null) {
            planPriceRepository.deleteByPlan(updatedPlan);
            for (AdminPlanDto.PlanPriceDto dto : requestDto.getPrices()) {
                PlanTermsEntity term = planTermsRepository.findByPtermMonth(dto.getTermMonth())
                        .orElseThrow(() -> new EntityNotFoundException("기간 정보를 찾을 수 없습니다."));
                PlanPriceEntity price = PlanPriceEntity.builder()
                        .plan(updatedPlan)
                        .pterm(term)
                        .ppriceBilMode(dto.getPpriceBilMode())
                        .ppriceAmount(dto.getPpriceAmount())
                        .ppriceCurr(dto.getPpriceCurr())
                        .ppriceActive(dto.getPpriceActive())
                        .build();
                planPriceRepository.save(price);
            }
        }

        // 혜택 정보 갱신 (기존 삭제 후 재저장)
        if (requestDto.getBenefits() != null) {
            planBenefitRepository.deleteByPlan(updatedPlan);
            for (AdminPlanDto.PlanBenefitDto dto : requestDto.getBenefits()) {
                PlanBenefitEntity benefit = PlanBenefitEntity.builder()
                        .plan(updatedPlan)
                        .pbPriceCap(dto.getPbPriceCap())
                        .pbNote(dto.getPbNote())
                        .build();
                planBenefitRepository.save(benefit);
            }
        }

        return buildPlanResponse(updatedPlan);
    }

    public void deletePlan(Long planId) {
        PlansEntity plan = plansRepository.findById(planId)
                .orElseThrow(() -> new EntityNotFoundException("Plan not found with id " + planId));

        planPriceRepository.deleteByPlan(plan);
        planBenefitRepository.deleteByPlan(plan);
        plansRepository.delete(plan);
    }

    // -------------------------------
    // ✅ 구독 관리
    // -------------------------------

    public List<PlanMember> getMemberSubscriptions(Long memberId) {
        return planMemberRepository.findByMnum(memberId);
    }

    public PlanMember updateSubscriptionStatus(Long pmId, String status) {
        PlanMember planMember = planMemberRepository.findById(pmId)
                .orElseThrow(() -> new EntityNotFoundException("Subscription not found with id " + pmId));

        PlanEnums.PmStatus newStatus = PlanEnums.PmStatus.valueOf(status.toUpperCase());
        planMember.setPmStat(newStatus);

        return planMemberRepository.save(planMember);
    }

    // -------------------------------
    // ✅ 결제 내역
    // -------------------------------

    public List<PlanInvoiceEntity> getMemberInvoices(Long memberId) {
        // 연관관계 필드명이 다르면 Repository 메서드명도 맞춰주세요.
        return planInvoiceRepository.findByPmld_Mnum(memberId);
    }

    public PlanInvoiceEntity getInvoiceDetail(Long piId) {
        return planInvoiceRepository.findById(piId)
                .orElseThrow(() -> new EntityNotFoundException("Invoice not found with id " + piId));
    }

    // -------------------------------
    // ✅ 결제 실패 로그
    // -------------------------------

    public List<PlanAttemptEntity> getInvoiceAttempts(Long piId) {
        return planAttemptRepository.findByPild_PiId(piId);
    }

    // -------------------------------
    // ✅ 환불 관리
    // -------------------------------

    public PlanRefundEntity createRefund(RefundRequestDTO refundRequest) {
        // 1) 결제건 로드 (FK: invoice)
        PlanInvoiceEntity invoice = planInvoiceRepository.findById(refundRequest.getPiId())
                .orElseThrow(() -> new EntityNotFoundException("Invoice not found. piId=" + refundRequest.getPiId()));

        // 2) (선택) 결제 시도 로그 로드 (요청에 attemptId가 있을 때만)
        PlanAttemptEntity attempt = null;
        if (refundRequest.getAttemptId() != null) {
            attempt = planAttemptRepository.findById(refundRequest.getAttemptId())
                    .orElseThrow(() -> new EntityNotFoundException("Attempt not found. pattId=" + refundRequest.getAttemptId()));
        }

        // 3) enum 파싱 (대소문자 안전)
        PrefType type = PrefType.valueOf(refundRequest.getType().toUpperCase());

        // 4) 금액 기본값 처리
        BigDecimal amount = (refundRequest.getAmount() != null) ? refundRequest.getAmount() : BigDecimal.ZERO;

        // 5) 환불 방법 (필수값) - Admin에서 직접 수행이면 MANUAL, 원결제수단 환불이면 ORIGINAL
        PrefMethod method = (refundRequest.getMethod() != null)
                ? PrefMethod.valueOf(refundRequest.getMethod().toUpperCase())
                : PrefMethod.ORIGINAL;

        // 6) 엔티티 생성 (빌더 사용 권장)
        PlanRefundEntity refund = PlanRefundEntity.builder()
                .invoice(invoice)                 // ✅ FK 주입
                .attempt(attempt)                 // ✅ 선택 주입
                .prefType(type)
                .prefReason(refundRequest.getReason())
                .prefAmount(amount)               // ✅ 필드명: prefAmount
                .prefCurr("KRW")                  // 기본 통화
                .prefStat(PrefStatus.REQUESTED)   // 초기 상태
                .prefMethod(method)               // ✅ NOT NULL
                .prefRequest(LocalDateTime.now())
                .build();

        return planRefundRepository.save(refund);
    }

    private AdminPlanDto.Response buildPlanResponse(PlansEntity plan) {
        List<PlanPriceEntity> prices = planPriceRepository.findByPlan(plan);
        List<PlanBenefitEntity> benefits = planBenefitRepository.findByPlan(plan);

        return AdminPlanDto.Response.builder()
                .planId(plan.getPlanId())
                .planName(plan.getPlanName().getPlanName())
                .planCode(plan.getPlanCode())
                .planActive(plan.getPlanActive())
                .planCreate(plan.getPlanCreate())
                .prices(prices.stream().map(AdminPlanDto.PlanPriceDto::fromEntity).toList())
                .benefits(benefits.stream().map(AdminPlanDto.PlanBenefitDto::fromEntity).toList())
                .build();
    }
	 // -------------------------------
	 // ✅ 환불 상태 업데이트
	 // -------------------------------
	 public PlanRefundEntity updateRefundStatus(Long refundId, String status) {
	     PlanRefundEntity refund = planRefundRepository.findById(refundId)
	             .orElseThrow(() -> new EntityNotFoundException("Refund not found with id " + refundId));
	
	     PlanEnums.PrefStatus newStatus = PlanEnums.PrefStatus.valueOf(status.toUpperCase());
	     refund.setPrefStat(newStatus);
	     refund.setPrefProcess(LocalDateTime.now());
	
	     return planRefundRepository.save(refund);
	 }
	
	 // -------------------------------
	 // ✅ 환불 목록 조회 (상태별 필터링 가능)
	 // -------------------------------
	 public List<PlanRefundEntity> getRefunds(String status) {
	     if (status != null && !status.isEmpty()) {
	         PlanEnums.PrefStatus enumStatus = PlanEnums.PrefStatus.valueOf(status.toUpperCase());
	         return planRefundRepository.findByPrefStat(enumStatus);
	     }
	     return planRefundRepository.findAll();
	 }
}
