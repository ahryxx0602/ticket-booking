package com.ticket.booking.application.usecase;

import java.time.LocalDateTime;

public interface ConfirmBookingUseCase {
    boolean execute(Long bookingId, LocalDateTime now);
}
