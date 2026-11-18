package com.ticket.payment.application.dto;

import com.ticket.payment.domain.model.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreatePaymentCommand {
    @NotNull
    private Long bookingId;

    @NotNull
    @DecimalMin("1000")
    private BigDecimal amount;

    @NotNull
    private PaymentMethod paymentMethod;
}
