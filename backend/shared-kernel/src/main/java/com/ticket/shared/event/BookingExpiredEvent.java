package com.ticket.shared.event;

import com.ticket.shared.domain.DomainEvent;

import java.time.Instant;

public record BookingExpiredEvent(
        String eventId,
        String bookingReference,
        Long bookingId,
        Long userId,
        String customerEmail,
        String customerPhone,
        Instant occurredOn
) implements DomainEvent {
    public BookingExpiredEvent {
        if (eventId == null || eventId.trim().isEmpty()) {
            throw new IllegalArgumentException("eventId cannot be null or empty");
        }
        if (bookingReference == null || bookingReference.trim().isEmpty()) {
            throw new IllegalArgumentException("bookingReference cannot be null or empty");
        }
        if (occurredOn == null) {
            occurredOn = Instant.now();
        }
    }
}
