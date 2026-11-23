package com.ticket.payment.application.dto;

import com.ticket.payment.domain.model.PaymentMethod;
import com.ticket.payment.domain.model.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDetailResponse {
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
    private boolean reconciliationRequired;
    private LocalDateTime createdAt;
}
