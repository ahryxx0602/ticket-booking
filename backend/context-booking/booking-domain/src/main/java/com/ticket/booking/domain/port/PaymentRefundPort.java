package com.ticket.booking.domain.port;

import java.math.BigDecimal;

public interface PaymentRefundPort {
    boolean refundByBookingId(Long bookingId, BigDecimal refundAmount, String reason);
}
