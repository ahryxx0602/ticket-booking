package com.ticket.payment.domain.port;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentBookingPort {

    Optional<BookingSnapshot> findBookingByReference(String bookingReference);
    Optional<BookingSnapshot> findBookingById(Long bookingId);

    record BookingSnapshot(
            Long id,
            String bookingReference,
            Long userId,
            BigDecimal totalAmount,
            String status,
            LocalDateTime expiresAt,
            Long tripId,
            String phone,
            String customerEmail,
            List<String> seatNumbers
    ) {
        public BookingSnapshot(Long id, String bookingReference, Long userId, BigDecimal totalAmount, String status, LocalDateTime expiresAt) {
            this(id, bookingReference, userId, totalAmount, status, expiresAt, null, null, null, List.of());
        }

        public boolean isHeld() {
            return "HELD".equalsIgnoreCase(status);
        }

        public boolean isExpired(LocalDateTime now) {
            return expiresAt != null && !expiresAt.isAfter(now);
        }
    }
}
