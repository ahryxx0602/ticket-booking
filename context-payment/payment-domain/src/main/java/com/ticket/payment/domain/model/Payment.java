package com.ticket.payment.domain.model;

import com.ticket.shared.domain.BaseEntity;
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

    public void markSuccess(String transactionId) {
        this.status = PaymentStatus.SUCCESS;
        this.transactionId = transactionId;
        this.paymentTime = LocalDateTime.now();
    }

    public void markFailed() {
        this.status = PaymentStatus.FAILED;
    }
}
