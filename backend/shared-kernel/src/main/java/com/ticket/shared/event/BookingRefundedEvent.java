package com.ticket.shared.event;

import com.ticket.shared.domain.DomainEvent;

import java.math.BigDecimal;
import java.time.Instant;

public record BookingRefundedEvent(
        String eventId,
        Long bookingId,
        String bookingReference,
        Long tripId,
        BigDecimal refundAmount,
        int refundPercentage,
        String reason,
        Instant occurredOn
) implements DomainEvent {
    public BookingRefundedEvent {
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
