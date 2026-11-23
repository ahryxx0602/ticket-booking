package com.ticket.catalog.domain.model;

import java.math.BigDecimal;

public enum SeatType {
    STANDARD(new BigDecimal("1.00")),
    VIP(new BigDecimal("1.50"));

    private final BigDecimal defaultMultiplier;

    SeatType(BigDecimal defaultMultiplier) {
        this.defaultMultiplier = defaultMultiplier;
    }

    public BigDecimal getDefaultMultiplier() {
        return defaultMultiplier;
    }

    public static SeatType fromString(String type) {
        if (type == null || type.trim().isEmpty()) {
            return STANDARD;
        }
        try {
            return SeatType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return STANDARD;
        }
    }
}
