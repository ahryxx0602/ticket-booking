package com.ticket.partner.domain.model;

public enum VehicleStatus {
    ACTIVE,
    MAINTENANCE,
    INACTIVE;

    public boolean isAvailable() {
        return this == ACTIVE;
    }
}
