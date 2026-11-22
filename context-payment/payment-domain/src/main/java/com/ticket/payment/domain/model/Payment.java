package com.ticket.payment.domain.model;

import com.ticket.shared.domain.BaseEntity;
import com.ticket.shared.exception.BusinessException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment extends BaseEntity {
    private Long id;
    private String paymentReference;
    private Long bookingId;
    private BigDecimal amount;
    private String currency;
    private PaymentMethod paymentMethod;
    private String transactionId;
    private PaymentStatus status;
    private LocalDateTime paymentTime;
    private BigDecimal refundAmount;
    private String refundReason;
    private LocalDateTime refundedAt;
    @Builder.Default
    private boolean reconciliationRequired = false;
    private Long version;

    public void markSuccess(String transactionId) {
        if (this.status != PaymentStatus.PENDING) {
            throw new BusinessException("INVALID_PAYMENT_STATUS", "Payment is not in PENDING status: " + this.status);
        }
        this.status = PaymentStatus.SUCCESS;
        this.transactionId = transactionId;
        this.paymentTime = LocalDateTime.now();
    }

    public void markFailed() {
        if (this.status != PaymentStatus.PENDING) {
            throw new BusinessException("INVALID_PAYMENT_STATUS", "Payment is not in PENDING status: " + this.status);
        }
        this.status = PaymentStatus.FAILED;
    }

    public void markReconciliationRequired(String transactionId, String reason) {
        if (this.status != PaymentStatus.PENDING && this.status != PaymentStatus.FAILED) {
            throw new BusinessException("INVALID_PAYMENT_STATUS", "Payment cannot be marked for reconciliation from status: " + this.status);
        }
        this.status = PaymentStatus.SUCCESS;
        this.transactionId = transactionId;
        this.reconciliationRequired = true;
        this.refundReason = reason;
        this.paymentTime = LocalDateTime.now();
    }

    public void markReconciliationRequired(String reason) {
        markReconciliationRequired(this.transactionId, reason);
    }

    public void markRefunded(BigDecimal refundAmount, String reason) {
        if (this.status != PaymentStatus.SUCCESS) {
            throw new BusinessException("INVALID_PAYMENT_STATUS", "Only SUCCESS payments can be refunded");
        }
        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0
                || (this.amount != null && refundAmount.compareTo(this.amount) > 0)) {
            throw new BusinessException("INVALID_REFUND_AMOUNT", "Refund amount must be positive and not exceed original payment amount");
        }
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("INVALID_REFUND_REASON", "Refund reason must not be blank");
        }
        this.status = PaymentStatus.REFUNDED;
        this.refundAmount = refundAmount;
        this.refundReason = reason.trim();
        this.refundedAt = LocalDateTime.now();
    }
}
