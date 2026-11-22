package com.ticket.shared.event;

import com.ticket.shared.domain.DomainEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PaymentConfirmedEvent(
        String eventId,
        String paymentReference,
        String bookingReference,
        Long bookingId,
        Long userId,
        Long tripId,
        String customerEmail,
        String customerPhone,
        String passengerName,
        BigDecimal totalAmount,
        List<String> seatNumbers,
        Instant occurredOn
) implements DomainEvent {
    public PaymentConfirmedEvent {
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
