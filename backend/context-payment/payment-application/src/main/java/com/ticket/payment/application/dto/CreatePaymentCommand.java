package com.ticket.payment.application.dto;

import com.ticket.payment.domain.model.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentCommand {
    @NotBlank(message = "Booking reference is required")
    @Pattern(regexp = "^[A-Za-z0-9_\\-]{6,36}$", message = "Invalid booking reference format")
    private String bookingReference;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;
}
