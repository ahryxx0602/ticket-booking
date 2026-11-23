package com.ticket.catalog.domain.model;

public enum TripStatus {
    SCHEDULED,
    DEPARTED,
    CANCELLED;

    public static TripStatus fromString(String status) {
        if (status == null || status.trim().isEmpty()) {
            return SCHEDULED;
        }
        try {
            return TripStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return SCHEDULED;
        }
    }
}
