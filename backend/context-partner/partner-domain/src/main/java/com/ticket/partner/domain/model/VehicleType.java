package com.ticket.partner.domain.model;

public enum VehicleType {
    SEAT,
    SLEEPER,
    LIMOUSINE;

    public static VehicleType fromString(String value) {
        if (value == null) return LIMOUSINE;
        try {
            return VehicleType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return LIMOUSINE;
        }
    }
}
