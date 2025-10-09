package com.dodam.plan.Entity;

import com.dodam.plan.enums.PlanEnums.PattResult;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "PlanAttempt",
        indexes = {
                @Index(name = "idx_attempt_invoice", columnList = "piId"),
                @Index(name = "idx_attempt_uid", columnList = "pattUid")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanAttemptEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long pattId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "piId", nullable = false)
    private PlanInvoiceEntity invoice;

    @Enumerated(EnumType.STRING)
    @Column(name = "pattResult", nullable = false, length = 16)
    private PattResult pattResult;

    @Column(name = "pattFail", length = 4000)
    private String pattFail;

    @Column(name = "pattUid", length = 128)
    private String pattUid;

    @Column(name = "pattUrl", length = 1024)
    private String pattUrl;

    @Lob
    @Column(name = "pattResponse")
    private String pattResponse;

    @CreationTimestamp
    @Column(name = "pattAt", updatable = false)
    private LocalDateTime pattAt;

    /* ========= 편의 메서드 ========= */

    public static PlanAttemptEntity success(PlanInvoiceEntity invoice, String paymentUid, String responseJson) {
        return PlanAttemptEntity.builder()
                .invoice(invoice)
                .pattResult(PattResult.SUCCESS)
                .pattUid(paymentUid)
                .pattResponse(responseJson)
                .build();
    }

    public static PlanAttemptEntity fail(PlanInvoiceEntity invoice, String failReason, String responseJson) {
        return PlanAttemptEntity.builder()
                .invoice(invoice)
                .pattResult(PattResult.FAIL)
                .pattFail(failReason)
                .pattResponse(responseJson)
                .build();
    }

    public static PlanAttemptEntity pending(PlanInvoiceEntity invoice, String orderId, String responseJson) {
        return PlanAttemptEntity.builder()
                .invoice(invoice)
                .pattResult(PattResult.PENDING)
                .pattUid(orderId)
                .pattResponse(responseJson)
                .build();
    }

    /* ====== 서비스 코드 호환용 편의 세터 ====== */

    /** "Y"/"N" 문자열로 성공여부 세팅 → pattResult 매핑 */
    public void setSuccessYn(String yn) {
        boolean ok = "Y".equalsIgnoreCase(yn);
        this.pattResult = ok ? PattResult.SUCCESS : PattResult.FAIL;
    }

    /** respUid → pattUid 에 매핑 */
    public void setRespUid(String uid) {
        this.pattUid = uid;
    }

    /** raw JSON → pattResponse 에 매핑 */
    public void setPattRaw(String raw) {
        this.pattResponse = raw;
    }
}
