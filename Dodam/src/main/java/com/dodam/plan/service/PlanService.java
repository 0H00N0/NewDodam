package com.dodam.plan.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

import com.dodam.plan.repository.PlanBenefitRepository;
import com.dodam.plan.repository.PlanPriceRepository;
import com.dodam.plan.repository.PlansRepository;
import com.dodam.plan.Entity.PlanBenefitEntity;
import com.dodam.plan.Entity.PlansEntity;
import com.dodam.plan.Entity.PlanPriceEntity;
import com.dodam.plan.dto.PlanDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlanService {
	private final PlansRepository pr;
	private final PlanBenefitRepository pbr;
	private final PlanPriceRepository ppr;

	/** 활성 플랜 목록 + 1:1 혜택 매칭 */
	public List<PlanDTO> getActivePlans() {
		List<PlansEntity> plans = pr.findByPlanActiveTrue();

		// 혜택 한꺼번에 매핑 (N+1 회피)
		List<PlanBenefitEntity> benefits = pbr.findByPlanIn(plans);
		Map<Long, PlanBenefitEntity> byPlanId = new HashMap<>();
		for (PlanBenefitEntity b : benefits) {
			byPlanId.put(b.getPlan().getPlanId(), b);
		}

		return plans.stream().map(p -> {
			PlanBenefitEntity b = byPlanId.get(p.getPlanId());

			// 활성 가격 조회 (planId 기준)
			List<PlanPriceEntity> prices = ppr.findByPlan_PlanIdAndPpriceActiveTrue(p.getPlanId());

			// ✅ months(= pterm.ptermMonth) 기준 정렬 (null은 마지막)
			List<PlanPriceEntity> sortedPrices = prices.stream()
					.sorted(Comparator.comparing(
							(PlanPriceEntity e) -> e.getPterm() != null ? e.getPterm().getPtermMonth() : null,
							Comparator.nullsLast(Integer::compareTo)))
					.toList();

			BenefitNoteParsed parsed = parseBenefitNote(b != null ? b.getPbNote() : null);

			// 정렬된 가격 리스트를 그대로 DTO에 전달
			return PlanDTO.of(p, b, sortedPrices, parsed.desc(), parsed.items());
		}).toList();
	}

	/** 코드로 단일 플랜 조회 */
	public PlanDTO getByCode(String code) {
		PlansEntity plan = pr.findByPlanCodeIgnoreCase(code)
				.orElseThrow(() -> new NoSuchElementException("플랜을 찾을 수 없습니다: " + code));

		PlanBenefitEntity benefit = pbr.findFirstByPlan(plan).orElse(null);

		List<PlanPriceEntity> prices = ppr.findByPlan_PlanIdAndPpriceActiveTrue(plan.getPlanId());

		// ✅ months(= pterm.ptermMonth) 기준 정렬 (null은 마지막)
		List<PlanPriceEntity> sortedPrices = prices.stream()
				.sorted(Comparator.comparing(
						(PlanPriceEntity e) -> e.getPterm() != null ? e.getPterm().getPtermMonth() : null,
						Comparator.nullsLast(Integer::compareTo)))
				.toList();

		BenefitNoteParsed parsed = parseBenefitNote(benefit != null ? benefit.getPbNote() : null);

		return PlanDTO.of(plan, benefit, sortedPrices, parsed.desc(), parsed.items());
	}

	// ───────────────────────────────────────────────────
	// pbNote 파싱: 설명(desc) + 불릿(items)
	// ───────────────────────────────────────────────────
	private BenefitNoteParsed parseBenefitNote(String pbNote) {
		if (pbNote == null || pbNote.isBlank()) {
			return new BenefitNoteParsed(null, List.of());
		}

		String[] lines = pbNote.replace("\r\n", "\n").split("\n");
		StringBuilder desc = new StringBuilder();
		List<String> items = new ArrayList<>();

		for (String raw : lines) {
			String line = raw == null ? "" : raw.trim();
			if (line.isEmpty())
				continue;

			if (line.startsWith("-") || line.startsWith("•")) {
				String txt = line.substring(1).trim();
				if (!txt.isEmpty())
					items.add(txt);
			} else {
				if (desc.length() > 0)
					desc.append(" ");
				desc.append(line);
			}
		}
		return new BenefitNoteParsed(desc.length() == 0 ? null : desc.toString(), items);
	}

	// DTO 조립용 내부 record
	private record BenefitNoteParsed(String desc, List<String> items) {
	}
}
