package com.ticket.booking.application.usecase;

import java.time.LocalDateTime;

public interface ConfirmBookingUseCase {
    boolean execute(Long bookingId, LocalDateTime now);

    enum ConfirmationOutcome {
        CONFIRMED,
        ALREADY_CONFIRMED,
        EXPIRED_OR_INVALID
    }

    default ConfirmationOutcome confirmWithOutcome(Long bookingId, LocalDateTime now) {
        return execute(bookingId, now) ? ConfirmationOutcome.CONFIRMED : ConfirmationOutcome.EXPIRED_OR_INVALID;
    }
}
