package com.dodam.plan.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.Map;

public interface PlanPortoneClientService {
	Map<String, Object> confirmIssueBillingKey(String billingIssueToken);

	record ConfirmRequest(String paymentId, String billingKey, long amountValue, String currency, // "KRW"
			String customerId, // null 허용
			String orderName, // null 허용
			boolean isTest) {
	}

	record ConfirmResponse(String id, String status, String raw) {
	}

	record LookupResponse(String id, String status, String raw) {
	}

	ConfirmResponse confirmByBillingKey(ConfirmRequest req);

	// PlanPortoneClientService.java
	JsonNode scheduleByBillingKey(
	    String paymentId,
	    String billingKey,
	    long amount,
	    String currency,
	    String customerId,
	    String orderName,
	    Instant timeToPayUtc
	);


	LookupResponse lookupPayment(String paymentId);

	JsonNode getPayment(String paymentId);

	JsonNode listPaymentsByBillingKey(String billingKey, int size);

	// /payments?paymentId=orderId 조회용
	JsonNode getPaymentByOrderId(String orderId);
}
