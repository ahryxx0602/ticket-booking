package com.ticket.catalog.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatDto {
    private Long id;
    private Long tripId;
    private String seatNumber;
    private String seatType;
    private BigDecimal priceMultiplier;
    private BigDecimal finalPrice;
}
