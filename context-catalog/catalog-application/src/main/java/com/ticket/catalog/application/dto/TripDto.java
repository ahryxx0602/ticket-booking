package com.ticket.catalog.application.dto;

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
public class TripDto {
    private Long id;
    private String tripCode;
    private String departureStationCode;
    private String departureStationName;
    private String departureCity;
    private String arrivalStationCode;
    private String arrivalStationName;
    private String arrivalCity;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private String transportType;
    private BigDecimal basePrice;
    private Integer totalSeats;
    private String status;
}
