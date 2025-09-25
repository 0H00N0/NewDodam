package com.dodam.admin.service;

import com.dodam.admin.dto.AdminPlanDto;
import com.dodam.plan.Entity.PlanBenefitEntity;
import com.dodam.plan.Entity.PlanNameEntity;
import com.dodam.plan.Entity.PlanPriceEntity;
import com.dodam.plan.Entity.PlanTermsEntity;
import com.dodam.plan.Entity.PlansEntity;
import com.dodam.plan.repository.PlanBenefitRepository;
import com.dodam.plan.repository.PlanNameRepository;
import com.dodam.plan.repository.PlanPriceRepository;
import com.dodam.plan.repository.PlanTermsRepository;
import com.dodam.plan.repository.PlansRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminPlanService {

    private final PlansRepository plansRepository;
    private final PlanNameRepository planNameRepository;
    private final PlanPriceRepository planPriceRepository;
    private final PlanTermsRepository planTermsRepository;
    private final PlanBenefitRepository planBenefitRepository; // PlanBenefitRepository 주입

    /**
     * 플랜 이름(텍스트)을 받아서 PlanNameEntity를 찾거나 새로 생성하는 헬퍼 메서드
     */
    private PlanNameEntity findOrCreatePlanName(String name) {
        return planNameRepository.findByPlanName(name)
                .orElseGet(() -> planNameRepository.save(PlanNameEntity.builder().planName(name).build()));
    }

    @Transactional
    public AdminPlanDto.Response createPlan(AdminPlanDto.CreateRequest requestDto) {
        // 1. 플랜 이름 처리
        PlanNameEntity planNameEntity = findOrCreatePlanName(requestDto.getPlanName());

        // 2. 기본 플랜 정보 저장
        PlansEntity newPlan = PlansEntity.builder()
                .planName(planNameEntity)
                .planCode(requestDto.getPlanCode())
                .planActive(requestDto.getPlanActive() != null ? requestDto.getPlanActive() : true)
                .build();
        PlansEntity savedPlan = plansRepository.save(newPlan);

        // 3. 가격 및 혜택 정보 저장
        savePricesForPlan(savedPlan, requestDto.getPrices());
        saveBenefitsForPlan(savedPlan, requestDto.getBenefits());

        // 4. 저장된 전체 정보를 DTO로 변환하여 반환
        return convertToDto(savedPlan);
    }

    public List<AdminPlanDto.Response> getAllPlans() {
        return plansRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public AdminPlanDto.Response getPlan(Long planId) {
        PlansEntity plan = plansRepository.findById(planId)
                .orElseThrow(() -> new EntityNotFoundException("Plan not found with id: " + planId));
        return convertToDto(plan);
    }

    @Transactional
    public AdminPlanDto.Response updatePlan(Long planId, AdminPlanDto.UpdateRequest requestDto) {
        // 1. 수정할 플랜 조회
        PlansEntity existingPlan = plansRepository.findById(planId)
                .orElseThrow(() -> new EntityNotFoundException("Plan not found with id: " + planId));

        // 2. 플랜 기본 정보 업데이트
        PlanNameEntity planNameEntity = findOrCreatePlanName(requestDto.getPlanName());
        existingPlan.setPlanName(planNameEntity);
        existingPlan.setPlanActive(requestDto.getPlanActive());

        // 3. 기존 가격/혜택 정보 삭제
        // 참고: Repository에 deleteByPlan_PlanId가 선언되어 있어야 합니다.
        planPriceRepository.deleteByPlan_PlanId(planId);
        planBenefitRepository.deleteByPlan_PlanId(planId);

        // 4. 새로운 가격/혜택 정보 저장
        savePricesForPlan(existingPlan, requestDto.getPrices());
        saveBenefitsForPlan(existingPlan, requestDto.getBenefits());

        return convertToDto(existingPlan);
    }

    @Transactional
    public void deletePlan(Long planId) {
        if (!plansRepository.existsById(planId)) {
            throw new EntityNotFoundException("Plan not found with id: " + planId);
        }
        // 자식 테이블(가격, 혜택 등)의 데이터를 먼저 삭제
        planPriceRepository.deleteByPlan_PlanId(planId);
        planBenefitRepository.deleteByPlan_PlanId(planId);
        plansRepository.deleteById(planId);
    }

    // --- Helper Methods ---

    /**
     * 가격 정보 리스트를 받아 데이터베이스에 저장하는 헬퍼 메서드
     */
    private void savePricesForPlan(PlansEntity plan, List<AdminPlanDto.PlanPriceDto> priceDtos) {
        if (priceDtos == null || priceDtos.isEmpty()) return;

        for (AdminPlanDto.PlanPriceDto priceDto : priceDtos) {
            PlanTermsEntity term = planTermsRepository.findByPtermMonth(priceDto.getTermMonth())
                    .orElseThrow(() -> new EntityNotFoundException("PlanTerm not found for month: " + priceDto.getTermMonth()));

            PlanPriceEntity priceEntity = PlanPriceEntity.builder()
                    .plan(plan)
                    .pterm(term)
                    .ppriceBilMode(priceDto.getPpriceBilMode())
                    .ppriceAmount(priceDto.getPpriceAmount())
                    .ppriceCurr(priceDto.getPpriceCurr())
                    .ppriceActive(priceDto.getPpriceActive() != null ? priceDto.getPpriceActive() : true)
                    .build();
            planPriceRepository.save(priceEntity);
        }
    }

    /**
     * 혜택 정보 리스트를 받아 데이터베이스에 저장하는 헬퍼 메서드 (신규 추가)
     */
    private void saveBenefitsForPlan(PlansEntity plan, List<AdminPlanDto.PlanBenefitDto> benefitDtos) {
        if (benefitDtos == null || benefitDtos.isEmpty()) return;

        for (AdminPlanDto.PlanBenefitDto benefitDto : benefitDtos) {
            PlanBenefitEntity benefitEntity = PlanBenefitEntity.builder()
                    .plan(plan)
                    .pbPriceCap(benefitDto.getPbPriceCap())
                    .pbNote(benefitDto.getPbNote())
                    .build();
            planBenefitRepository.save(benefitEntity);
        }
    }

    /**
     * Entity를 DTO로 변환하는 중앙 관리 메서드 (수정됨)
     */
    private AdminPlanDto.Response convertToDto(PlansEntity plan) {
        // 가격 정보 조회 및 DTO 변환
        List<PlanPriceEntity> prices = planPriceRepository.findByPlan_PlanId(plan.getPlanId());
        List<AdminPlanDto.PlanPriceDto> priceDtos = prices.stream()
                .map(AdminPlanDto.PlanPriceDto::fromEntity)
                .collect(Collectors.toList());

        // 혜택 정보 조회 및 DTO 변환 (추가)
        List<PlanBenefitEntity> benefits = planBenefitRepository.findByPlan_PlanId(plan.getPlanId());
        List<AdminPlanDto.PlanBenefitDto> benefitDtos = benefits.stream()
                .map(AdminPlanDto.PlanBenefitDto::fromEntity)
                .collect(Collectors.toList());

        return AdminPlanDto.Response.builder()
                .planId(plan.getPlanId())
                .planName(plan.getPlanName().getPlanName())
                .planCode(plan.getPlanCode())
                .planActive(plan.getPlanActive())
                .planCreate(plan.getPlanCreate())
                .prices(priceDtos)
                .benefits(benefitDtos) // 혜택 정보 포함
                .build();
    }
}