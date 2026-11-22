package com.ticket.notification.domain.port;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationBookingPort {
    record BookingDetailsSnapshot(
            Long bookingId,
            String bookingReference,
            Long userId,
            Long tripId,
            String status,
            BigDecimal totalAmount,
            String customerPhone,
            LocalDateTime expiresAt,
            List<String> seatNumbers
    ) {}

    Optional<BookingDetailsSnapshot> getBookingDetails(String bookingReference);
    Optional<BookingDetailsSnapshot> getBookingDetailsByReferenceAndPhone(String bookingReference, String phone);
}
