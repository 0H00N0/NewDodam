package com.dodam.plan.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

/**
 * PortOne 결제 응답 매핑 DTO
 * 응답은 수시로 필드가 늘어날 수 있으므로 unknown 필드는 무시합니다.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlanConfirmResDTO {
    private String id;        // 결제 고유 ID (예: pay_...)
    private String status;    // 예: PAID, FAILED, CANCELLED ...
    private Receipt receipt;  // 영수증 정보 (url 등)
    private Failure failure;  // 실패시 사유(code/message)가 여기로 내려오는 경우가 많음

    @Getter @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Receipt {
        private String url; // 영수증 링크
    }

    @Getter @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Failure {
        private String code;
        private String message;
    }

    /* ========= 편의 메서드 ========= */

    /** 결제 성공 여부 (status 기준) */
    @JsonIgnore
    public boolean isSuccess() {
        return "PAID".equalsIgnoreCase(this.status);
    }

    /** 영수증 URL (없으면 null) */
    @JsonIgnore
    public String getReceiptUrl() {
        return this.receipt != null ? this.receipt.getUrl() : null;
    }

    /** 실패 사유(사람이 볼 문구) - 없으면 status 반환 */
    @JsonIgnore
    public String getFailureReason() {
        if (this.failure != null && this.failure.getMessage() != null && !this.failure.getMessage().isBlank()) {
            return this.failure.getMessage();
        }
        return this.status; // 최소한 상태라도 남기자
    }
}
