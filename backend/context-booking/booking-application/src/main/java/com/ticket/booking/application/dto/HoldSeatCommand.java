package com.ticket.booking.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class HoldSeatCommand {

    // Optional: server resolves userId from JWT Principal; if provided, checked against JWT to prevent IDOR
    private Long userId;

    // Optional: client-provided request idempotency key (header Idempotency-Key or body property)
    @Size(max = 64, message = "Client request ID must not exceed 64 characters")
    private String clientRequestId;

    @NotNull(message = "Trip ID is required")
    private Long tripId;

    @NotEmpty(message = "At least one seat number must be selected")
    @Size(min = 1, max = 10, message = "You can hold between 1 and 10 seats per booking")
    private List<
            @NotBlank(message = "Seat number must not be blank")
            @Pattern(regexp = "^[A-Za-z0-9]{2,10}$", message = "Invalid seat number format")
            String> seatNumbers;
}
