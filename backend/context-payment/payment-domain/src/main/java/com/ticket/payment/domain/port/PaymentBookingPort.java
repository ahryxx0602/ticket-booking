package com.ticket.payment.domain.port;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentBookingPort {

    Optional<BookingSnapshot> findBookingByReference(String bookingReference);

    /**
     * BE-PAY-001: Khóa bi quan dòng Booking (SELECT ... FOR UPDATE) khi khởi tạo giao dịch thanh toán,
     * chống race condition tạo trùng 2 bản ghi Payment PENDING khi người dùng nhấn đúp nút thanh toán.
     */
    default Optional<BookingSnapshot> findBookingByReferenceForUpdate(String bookingReference) {
        return findBookingByReference(bookingReference);
    }

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
