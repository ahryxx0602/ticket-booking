package com.ticket.booking.domain.port;

public interface BookingTicketQrPort {
    String buildQrPayload(String bookingReference, Long tripId);
    String generateQrCodeBase64(String content);
}
