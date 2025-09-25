package com.dodam.plan.service;

import com.dodam.plan.Entity.PlanInvoiceEntity;
import com.dodam.plan.Entity.PlanMember;
import com.dodam.plan.enums.PlanEnums.PiStatus;
import com.dodam.plan.enums.PlanEnums.PmStatus;
import com.dodam.plan.repository.PlanInvoiceRepository;
import com.dodam.plan.repository.PlanMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlanSubscriptionSchedulerService {

    private final PlanMemberRepository pmRepo;
    private final PlanInvoiceRepository invoiceRepo;
    private final PlanPaymentOrchestratorService orchestrator;

    /**
     * 매 5분마다 도래한 구독의 다음 청구서를 생성하고 자동 결제 시도
     */
    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void run() {
        var due = pmRepo.findByPmNextBilBeforeAndPmStat(LocalDateTime.now(), PmStatus.ACTIVE);
        for (var pm : due) {
            var next = createNextInvoice(pm);
            if (next != null) {
                orchestrator.tryPayInvoice(next.getPiId()); // 내부에서 payWithBillingKey 호출
            }
        }
    }

    private PlanInvoiceEntity createNextInvoice(PlanMember pm) {
        // TODO: 플랜 가격/주기 기반으로 기간 산출
        var inv = new PlanInvoiceEntity();
        inv.setPlanMember(pm);
        inv.setPiAmount(pm.getPrice().getPpriceAmount());
        inv.setPiUid("inv_" + UUID.randomUUID());
        inv.setPiStat(PiStatus.PENDING);
        return invoiceRepo.save(inv);
    }
}
