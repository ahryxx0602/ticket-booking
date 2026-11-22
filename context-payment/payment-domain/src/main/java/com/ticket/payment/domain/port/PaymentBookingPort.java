package com.ticket.payment.domain.port;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

public interface PaymentBookingPort {

    Optional<BookingSnapshot> findBookingByReference(String bookingReference);

    record BookingSnapshot(
            Long id,
            String bookingReference,
            Long userId,
            BigDecimal totalAmount,
            String status,
            LocalDateTime expiresAt
    ) {
        public boolean isHeld() {
            return "HELD".equalsIgnoreCase(status);
        }

        public boolean isExpired(LocalDateTime now) {
            return expiresAt != null && !expiresAt.isAfter(now);
        }
    }
}
