package com.ticket.catalog.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Seat {
    private Long id;
    private Long tripId;
    private String seatNumber;
    private SeatType seatType;
    private BigDecimal priceMultiplier;

    public BigDecimal calculatePrice(BigDecimal basePrice) {
        if (basePrice == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal multiplier = this.priceMultiplier != null
                ? this.priceMultiplier
                : (this.seatType != null ? this.seatType.getDefaultMultiplier() : BigDecimal.ONE);
        return basePrice.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }

    public boolean isVip() {
        return SeatType.VIP.equals(this.seatType);
    }
}
