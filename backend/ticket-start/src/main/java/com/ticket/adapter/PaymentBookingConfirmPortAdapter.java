package com.ticket.adapter;

import com.ticket.booking.application.usecase.ConfirmBookingUseCase;
import com.ticket.booking.domain.model.Booking;
import com.ticket.booking.domain.model.BookingStatus;
import com.ticket.booking.domain.repository.BookingRepository;
import com.ticket.payment.domain.port.PaymentBookingConfirmPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentBookingConfirmPortAdapter implements PaymentBookingConfirmPort {

    private final ConfirmBookingUseCase confirmBookingUseCase;
    private final BookingRepository bookingRepository;

    @Override
    public BookingConfirmResult confirmBooking(Long bookingId, LocalDateTime now) {
        try {
            boolean confirmed = confirmBookingUseCase.execute(bookingId, now);
            if (confirmed) {
                return BookingConfirmResult.CONFIRMED;
            }
        } catch (Exception ex) {
            log.warn("Exception during booking confirmation for bookingId {}: {}", bookingId, ex.getMessage());
        }

        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking != null && booking.getStatus() == BookingStatus.CONFIRMED) {
            return BookingConfirmResult.ALREADY_CONFIRMED;
        }
        return BookingConfirmResult.EXPIRED_RECONCILIATION_REQUIRED;
    }
}
