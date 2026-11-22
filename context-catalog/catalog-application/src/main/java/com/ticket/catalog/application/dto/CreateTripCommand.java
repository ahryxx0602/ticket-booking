package com.ticket.catalog.application.dto;

import com.ticket.catalog.domain.model.TransportType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
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
public class CreateTripCommand {
    @NotBlank(message = "Trip code is required")
    @Pattern(regexp = "^[A-Za-z0-9_-]{3,20}$", message = "Trip code must be 3-20 alphanumeric/hyphen characters")
    private String tripCode;

    @NotNull(message = "Departure station ID is required")
    private Long departureStationId;

    @NotNull(message = "Arrival station ID is required")
    private Long arrivalStationId;

    @NotNull(message = "Departure time is required")
    private LocalDateTime departureTime;

    @NotNull(message = "Arrival time is required")
    private LocalDateTime arrivalTime;

    @NotNull(message = "Transport type is required")
    private TransportType transportType;

    @NotNull(message = "Base price is required")
    @Positive(message = "Base price must be greater than zero")
    private BigDecimal basePrice;

    @Min(value = 1, message = "Total rows must be at least 1")
    @Max(value = 60, message = "Total rows cannot exceed 60")
    private int totalRows;

    @Min(value = 1, message = "Seats per row must be at least 1")
    @Max(value = 10, message = "Seats per row cannot exceed 10")
    private int seatsPerRow;

    @Min(value = 0, message = "VIP rows cannot be negative")
    private int vipRows;
}
