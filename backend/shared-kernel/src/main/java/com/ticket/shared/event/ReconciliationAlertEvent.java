package com.ticket.shared.event;

import com.ticket.shared.domain.DomainEvent;

import java.math.BigDecimal;
import java.time.Instant;

public record ReconciliationAlertEvent(
        String eventId,
        String paymentReference,
        String bookingReference,
        String reason,
        BigDecimal amount,
        Instant occurredOn
) implements DomainEvent {
    public ReconciliationAlertEvent {
        if (eventId == null || eventId.trim().isEmpty()) {
            throw new IllegalArgumentException("eventId cannot be null or empty");
        }
        if (paymentReference == null || paymentReference.trim().isEmpty()) {
            throw new IllegalArgumentException("paymentReference cannot be null or empty");
        }
        if (occurredOn == null) {
            occurredOn = Instant.now();
        }
    }
}
