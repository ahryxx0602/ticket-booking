package com.ticket.adapter;

import com.ticket.booking.domain.port.BookingTicketQrPort;
import com.ticket.notification.infrastructure.mail.QrCodeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingTicketQrPortAdapter implements BookingTicketQrPort {

    private final QrCodeUtil qrCodeUtil;

    @Override
    public String buildQrPayload(String bookingReference, Long tripId) {
        return qrCodeUtil.buildQrPayload(bookingReference, tripId, null);
    }

    @Override
    public String generateQrCodeBase64(String content) {
        return qrCodeUtil.generateQrCodeBase64(content);
    }
}
