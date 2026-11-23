package com.ticket.booking.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminRefundCommand {

    @NotBlank(message = "Refund reason is required")
    @Size(max = 255, message = "Refund reason must not exceed 255 characters")
    private String reason;

    @NotNull(message = "Refund percentage is required")
    @Min(value = 1, message = "Refund percentage must be between 1 and 100")
    @Max(value = 100, message = "Refund percentage must be between 1 and 100")
    private Integer refundPercentage;
}
