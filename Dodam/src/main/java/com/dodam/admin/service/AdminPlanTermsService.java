package com.dodam.admin.service;

import com.dodam.plan.Entity.PlanTermsEntity;
import com.dodam.plan.repository.PlanTermsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminPlanTermsService {

    private final PlanTermsRepository planTermsRepository;

    public List<PlanTermsEntity> getAllTerms() {
        return planTermsRepository.findAll();
    }

    public PlanTermsEntity getTermById(Long id) {
        return planTermsRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 ptermId: " + id));
    }

    public PlanTermsEntity getTermByMonth(Integer month) {
        return planTermsRepository.findByPtermMonth(month)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 ptermMonth: " + month));
    }
}
