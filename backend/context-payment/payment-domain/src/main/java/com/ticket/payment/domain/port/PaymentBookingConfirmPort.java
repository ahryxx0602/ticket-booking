package com.ticket.payment.domain.port;

import java.time.LocalDateTime;

public interface PaymentBookingConfirmPort {

    BookingConfirmResult confirmBooking(Long bookingId, LocalDateTime now);

    enum BookingConfirmResult {
        CONFIRMED,
        ALREADY_CONFIRMED,
        EXPIRED_RECONCILIATION_REQUIRED
    }
}
