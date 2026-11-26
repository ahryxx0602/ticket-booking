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
public class CustomerPaymentStatusResponse {
    private String bookingReference;
    private String bookingStatus;
    private boolean hasPaymentAttempt;
    private String paymentReference;
    private PaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;
    private BigDecimal amount;
    private String currency;
    private boolean reconciliationRequired;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
