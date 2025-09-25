// src/main/java/com/dodam/plan/controller/PlanPaymentMethodController.java
package com.dodam.plan.controller;

import com.dodam.plan.dto.PlanCardMeta;
import com.dodam.plan.dto.PlanPaymentRegisterReq;
import com.dodam.plan.Entity.PlanPaymentEntity;
import com.dodam.plan.repository.PlanPaymentRepository;
import com.dodam.plan.service.PlanPaymentGatewayService;
import com.dodam.plan.service.PlanPaymentProfileService;
import com.dodam.plan.service.PlanPortoneClientService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/billing-keys")
public class PlanPaymentMethodController {

	private final PlanPaymentRepository paymentRepo;
	private final PlanPaymentProfileService profileSvc;
	private final PlanPaymentGatewayService pgSvc;
	private final PlanPortoneClientService portoneClient;

	// confirm 응답 파싱용
	private final ObjectMapper mapper = new ObjectMapper();

	/*
	 * ----------------------------------------------------------- 목록
	 * -----------------------------------------------------------
	 */
	@GetMapping("/list")
	public ResponseEntity<?> list(HttpSession session) {
		String mid = (String) session.getAttribute("sid");
		if (!StringUtils.hasText(mid)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "LOGIN_REQUIRED"));
		}
		var list = paymentRepo.findAllByMid(mid);
		var arr = list.stream().map(PlanPaymentMethodController::toMap).toList();
		return ResponseEntity.ok(arr);
	}

	/*
	 * ----------------------------------------------------------- 등록(멱등) : 동일
	 * billingKey가 있으면 소유자 같을 때 OK로 응답
	 * -----------------------------------------------------------
	 */
	@PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> register(@Valid @RequestBody PlanPaymentRegisterReq req, HttpSession session) {
		String mid = (String) session.getAttribute("sid");
		if (!StringUtils.hasText(mid)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "LOGIN_REQUIRED"));
		}

		String billingKey = StringUtils.trimAllWhitespace(req.getBillingKey());
		if (!StringUtils.hasText(billingKey)) {
			return ResponseEntity.badRequest().body(Map.of("error", "MISSING_BILLING_KEY"));
		}

		if (billingKey.startsWith("billing-issue-token")) {
			return ResponseEntity.badRequest().body(Map.of("error", "TEMP_TOKEN_NOT_ALLOWED", "message",
					"billingIssueToken은 등록할 수 없습니다. confirm 후 발급된 billingKey만 허용됩니다."));
		}
		if (!billingKey.startsWith("billing-key-")) {
			return ResponseEntity.badRequest().body(Map.of("error", "INVALID_BILLING_KEY_FORMAT"));
		}

		// ✅ 등록 시점에는 클라이언트 rawJson을 신뢰하지 않는다.
		//      (이전 카드의 응답이 남아있을 수 있음)
		//      원천적으로 null 메타에서 시작하고, confirm 단계에서
		//      세션에 캐시한 메타만 보강한다.
		PlanCardMeta meta = new PlanCardMeta(null, // billingKey
				null, // brand
				null, // bin
				null, // last4
				null, // pg
				false, // issued
				req.getCustomerId() // customerId는 요청값 보존
		);

		// confirm 단계에서 저장한 세션 캐시가 있으면 그 값으로만 보강
		@SuppressWarnings("unchecked")
		Map<String,String> cached = (Map<String,String>) session.getAttribute(sessionKeyFor(billingKey));
		if (cached != null && !cached.isEmpty()) {
		    safeMergeMetaMap(meta, cached);
		}
		// (선택 방어) rawJson을 보내왔고 해당 rawJson 내부 billingKey가 이번 billingKey와 "정확히 일치"하면 그때만 추가 보강
		try {
		    if (org.springframework.util.StringUtils.hasText(req.getRawJson())) {
		        com.fasterxml.jackson.databind.JsonNode raw = mapper.readTree(req.getRawJson());
		        String keyInRaw = raw.path("billingKey").asText(null);
		        if (billingKey.equals(keyInRaw)) {
		            Map<String,String> metaFromRaw = extractMetaFromConfirm(raw);
		            if (metaFromRaw != null && !metaFromRaw.isEmpty()) {
		                safeMergeMetaMap(meta, metaFromRaw);
		            }
		        }
		    }
		} catch (Exception ignore) {
		    // raw 파싱 실패는 무시 (등록 진행)
		}

		var existingOpt = paymentRepo.findByPayKey(billingKey);
		if (existingOpt.isPresent()) {
			var existing = existingOpt.get();
			if (!Objects.equals(existing.getMid(), mid)) {
				return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "OWNED_BY_ANOTHER_USER"));
			}
			safeMergeMeta(existing, meta);
			if (!StringUtils.hasText(existing.getPayRaw()) && StringUtils.hasText(req.getRawJson())) {
				existing.setPayRaw(req.getRawJson());
			}
			try {
				paymentRepo.save(existing);
				session.removeAttribute(sessionKeyFor(billingKey));

				// ✅ 등록 직후 카드 메타 업데이트 시도
				tryUpdateCardMeta(existing.getPayId(), billingKey);

				return ResponseEntity.ok(toMap(existing));
			} catch (Exception e) {
				log.error("REGISTER(merge) FAIL mid={} key={} ex={}", mid, billingKey, e.toString());
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
						.body(Map.of("error", "INTERNAL_SERVER_ERROR"));
			}
		}

		// 신규 저장
		String customerId = Optional.ofNullable(meta.getCustomerId()).filter(StringUtils::hasText).orElse(mid);
		var e = PlanPaymentEntity.builder().mid(mid).payKey(billingKey).payCustomer(customerId)
				.payCreatedAt(LocalDateTime.now()).payRaw(req.getRawJson()).build();
		safeMergeMeta(e, meta);

		try {
			paymentRepo.save(e);
			session.removeAttribute(sessionKeyFor(billingKey));

			// ✅ 신규 저장 직후 카드 메타 업데이트 시도
			tryUpdateCardMeta(e.getPayId(), billingKey);

			return ResponseEntity.ok(toMap(e));
		} catch (DataIntegrityViolationException dup) {
			String msg = String.valueOf(dup.getMostSpecificCause());
			if (msg != null && msg.contains("UK_PLANPAYMENT_MID_KEY")) {
				var again = paymentRepo.findByPayKey(billingKey).orElse(null);
				if (again != null && Objects.equals(again.getMid(), mid)) {
					return ResponseEntity.ok(toMap(again));
				}
				return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "DUPLICATE_BILLING_KEY"));
			}
			if (msg != null && msg.contains("ORA-01400") && msg.contains("PAYCUSTOMER")) {
				return ResponseEntity.badRequest().body(Map.of("error", "MISSING_PAYCUSTOMER"));
			}
			log.warn("register constraint violation: {}", msg);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "CONSTRAINT_VIOLATION"));
		} catch (Exception ex) {
			log.error("REGISTER FAIL mid={} key={} rawLen={} ex={}", mid, billingKey,
					(req.getRawJson() == null ? 0 : req.getRawJson().length()), ex.toString());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("error", "INTERNAL_SERVER_ERROR"));
		}
	}

	/*
	 * ----------------------------------------------------------- confirm : 200/409
	 * 모두 ISSUED로 정규화, 메타 세션 캐시
	 * -----------------------------------------------------------
	 */
	@PostMapping("/confirm")
	public ResponseEntity<?> confirm(@RequestBody ConfirmReq req, HttpSession session) {
		if (req == null || !StringUtils.hasText(req.getBillingIssueToken())) {
			return ResponseEntity.badRequest().body(Map.of("error", "MISSING_BILLING_ISSUE_TOKEN"));
		}
		try {
			// ✅ confirm 요청 로그
			log.info("confirm req token={}", req.getBillingIssueToken());
			Map<String, Object> result = portoneClient.confirmIssueBillingKey(req.getBillingIssueToken());
			// ✅ confirm 응답 로그
			log.info("confirm result={}", result);
			// 여기까지 오면 200 또는 409(이미 발급) 모두 'ISSUED'로 정규화됨
			String billingKey = (String) result.get("billingKey");

			// 세션 메타 캐시 (등록 시 보강에 사용)
			if (result.get("_raw") != null) {
				JsonNode json = mapper.valueToTree(result.get("_raw"));
				Map<String, String> metaMap = extractMetaFromConfirm(json);
				if (!metaMap.isEmpty() && StringUtils.hasText(billingKey)) {
					session.setAttribute(sessionKeyFor(billingKey), metaMap);
					log.debug("cached meta for billingKey {} -> {}", billingKey, metaMap);
				}
			}

			Map<String, Object> ok = new HashMap<>();
			ok.put("status", "ISSUED");
			if (StringUtils.hasText(billingKey))
				ok.put("billingKey", billingKey);
			return ResponseEntity.ok(ok);

		} catch (Exception e) {
			log.error("confirmIssueBillingKey failed: {}", e.toString());
			return ResponseEntity.status(502).body(Map.of("error", "CONFIRM_FAILED"));
		}
	}

	/*
	 * ----------------------------------------------------------- helpers
	 * -----------------------------------------------------------
	 */
	private static Map<String, Object> toMap(PlanPaymentEntity e) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", e.getPayId());
		m.put("billingKey", e.getPayKey());
		m.put("brand", e.getPayBrand());
		m.put("bin", e.getPayBin());
		m.put("last4", e.getPayLast4());
		m.put("pg", e.getPayPg());
		m.put("createdAt", e.getPayCreatedAt() == null ? null : e.getPayCreatedAt().toString());
		return m;
	}

	// PortOne raw에서 카드 메타 추출 시 예외 삼키고 기본값
	private PlanCardMeta safeExtract(String rawJson) {
		try {
			return pgSvc.extractCardMeta(rawJson);
		} catch (Exception ex) {
			log.warn("extractCardMeta failed, continue without meta: {}", ex.toString());
			// (프로젝트의 record/생성자 시그니처에 맞추어 초기값 전달)
			return new PlanCardMeta(null, // billingKey
					null, // brand
					null, // bin
					null, // last4
					null, // pg
					false, // issued
					null // customerId
			);
		}
	}

	// 엔티티에 메타 병합(값 없는 필드만 채움)
	private void safeMergeMeta(PlanPaymentEntity e, PlanCardMeta meta) {
		if (meta == null)
			return;
		if (!StringUtils.hasText(e.getPayBrand()) && StringUtils.hasText(meta.getBrand()))
			e.setPayBrand(meta.getBrand());
		if (!StringUtils.hasText(e.getPayBin()) && StringUtils.hasText(meta.getBin()))
			e.setPayBin(meta.getBin());
		if (!StringUtils.hasText(e.getPayLast4()) && StringUtils.hasText(meta.getLast4()))
			e.setPayLast4(meta.getLast4());
		if (!StringUtils.hasText(e.getPayPg()) && StringUtils.hasText(meta.getPg()))
			e.setPayPg(meta.getPg());
	}

	// PlanCardMeta에 비어있을 때만 세션 캐시(Map) 값으로 보강
	private void safeMergeMetaMap(PlanCardMeta meta, Map<String, String> map) {
		if (meta == null || map == null)
			return;
		if (!StringUtils.hasText(meta.getBrand()) && StringUtils.hasText(map.get("brand")))
			meta.setBrand(map.get("brand"));
		if (!StringUtils.hasText(meta.getBin()) && StringUtils.hasText(map.get("bin")))
			meta.setBin(map.get("bin"));
		if (!StringUtils.hasText(meta.getLast4()) && StringUtils.hasText(map.get("last4")))
			meta.setLast4(map.get("last4"));
		if (!StringUtils.hasText(meta.getPg()) && StringUtils.hasText(map.get("pg")))
			meta.setPg(map.get("pg"));
	}

	// confirm 응답(JSON) → 간단 메타 Map 추출
	private Map<String, String> extractMetaFromConfirm(JsonNode root) {
		Map<String, String> out = new HashMap<>();
		if (root == null)
			return out;
		JsonNode method = root.path("method");
		JsonNode card = method.path("card");
		putIfHasText(out, "bin", card.path("bin").asText(null));
		putIfHasText(out, "brand", card.path("brand").asText(null));
		putIfHasText(out, "last4", card.path("last4").asText(null));
		String pg = firstNonBlank(root.path("pgProvider").asText(null), root.path("pgCompany").asText(null));
		putIfHasText(out, "pg", pg);
		return out;
	}

	private static void putIfHasText(Map<String, String> m, String k, String v) {
		if (v != null && !v.isBlank())
			m.put(k, v);
	}

	private static String firstNonBlank(String... arr) {
		if (arr == null)
			return null;
		for (String s : arr) {
			if (s != null && !s.isBlank())
				return s;
		}
		return null;
	}

	private static String sessionKeyFor(String billingKey) {
		return "bkmeta:" + billingKey;
	}

	@Data
	public static class ConfirmReq {
		private String billingIssueToken;
	}

	/*
	 * ----------------------------------------------------------- 카드 메타 즉시 업데이트
	 * helper -----------------------------------------------------------
	 */
	private void tryUpdateCardMeta(Long payId, String billingKey) {
	    try {
	        var look = portoneClient.lookupPayment(billingKey); // 기존 그대로 둠
	        PlanCardMeta cardMeta = pgSvc.extractCardMeta(look.raw());

	        // ✅ 방어: 조회 결과의 billingKey가 다르면 오염 차단
	        if (cardMeta == null || cardMeta.getBillingKey() == null ||
	            !billingKey.equals(cardMeta.getBillingKey())) {
	            log.warn("[REGISTER] skip meta update: mismatched billingKey. asked={}, got={}",
	                     billingKey, (cardMeta == null ? null : cardMeta.getBillingKey()));
	            return; // ← 업데이트 안 함 (NULL 유지)
	        }

	        paymentRepo.updateCardMeta(
	            payId,
	            StringUtils.hasText(cardMeta.getBin())   ? cardMeta.getBin()   : null,
	            StringUtils.hasText(cardMeta.getBrand()) ? cardMeta.getBrand() : null,
	            StringUtils.hasText(cardMeta.getLast4()) ? cardMeta.getLast4() : null,
	            StringUtils.hasText(cardMeta.getPg())    ? cardMeta.getPg()    : null
	        );
	        log.info("[REGISTER] cardMeta updated for payId={} bin={} brand={} last4={} pg={}",
	                 payId, cardMeta.getBin(), cardMeta.getBrand(), cardMeta.getLast4(), cardMeta.getPg());
	    } catch (Exception e) {
	        log.warn("[REGISTER] cardMeta update skipped for payId={}, ex={}", payId, e.toString());
	    }
	}

	// 빌링키 발급 confirm 이후 저장하는 지점 예시
	private void saveIssuedCardMeta(String rawJson, String fallbackPayIdOrNull) {
		// 1) 메타 파싱
		PlanCardMeta meta = pgSvc.extractCardMeta(rawJson);

		// 2) billingKey 우선
		if (meta != null && StringUtils.hasText(meta.getBillingKey())) {
			int rows = paymentRepo.updateCardMetaByKey(meta.getBillingKey(),
					(StringUtils.hasText(meta.getBin()) ? meta.getBin() : null),
					(StringUtils.hasText(meta.getBrand()) ? meta.getBrand() : null),
					(StringUtils.hasText(meta.getLast4()) ? meta.getLast4() : null),
					(StringUtils.hasText(meta.getPg()) ? meta.getPg() : null));
			log.info("[PMCtrl] update by key rows={}, key={}", rows, meta.getBillingKey());
			if (rows > 0)
				return;
		}

		// 3) fallback: payId 로 시도 (상황에 따라 없을 수 있음)
		if (StringUtils.hasText(fallbackPayIdOrNull)) {
			Long payId = Long.valueOf(fallbackPayIdOrNull);
			int rows = paymentRepo.updateCardMeta(payId, (StringUtils.hasText(meta.getBin()) ? meta.getBin() : null),
					(StringUtils.hasText(meta.getBrand()) ? meta.getBrand() : null),
					(StringUtils.hasText(meta.getLast4()) ? meta.getLast4() : null),
					(StringUtils.hasText(meta.getPg()) ? meta.getPg() : null));
			log.info("[PMCtrl] update by id rows={}, payId={}", rows, payId);
		}
	}
}
