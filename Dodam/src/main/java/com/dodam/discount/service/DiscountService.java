package com.dodam.discount.service;

import com.dodam.discount.entity.Discount;
import com.dodam.discount.repository.DiscountRepository;
import com.dodam.plan.Entity.PlanTermsEntity;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DiscountService {

    private final DiscountRepository discountRepository;
    private final jakarta.persistence.EntityManager em;

    public List<Discount> findAll() {
        return discountRepository.findAll();
    }

    public Discount findById(Long id) {
        return discountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("할인율을 찾을 수 없습니다: " + id));
    }

    public Discount create(Integer disLevel, Integer disValue, Long ptermId) {
        PlanTermsEntity term = em.find(PlanTermsEntity.class, ptermId);
        if (term == null) throw new EntityNotFoundException("PlanTerms 없음: " + ptermId);

        Discount discount = Discount.builder()
                .disLevel(disLevel)
                .disValue(disValue)
                .ptermId(term)
                .build();

        return discountRepository.save(discount);
    }

    public Discount update(Long id, Integer disLevel, Integer disValue, Long ptermId) {
        Discount discount = findById(id);

        PlanTermsEntity term = em.find(PlanTermsEntity.class, ptermId);
        if (term == null) throw new EntityNotFoundException("PlanTerms 없음: " + ptermId);

        discount.setDisLevel(disLevel);
        discount.setDisValue(disValue);
        discount.setPtermId(term);

        return discountRepository.save(discount);
    }

    public void delete(Long id) {
        discountRepository.deleteById(id);
    }
}
