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
        // BE-PAY-002 & BE-TXN-001: Gọi confirmWithOutcome để phân biệt rõ CONFIRMED vs ALREADY_CONFIRMED vs EXPIRED_OR_INVALID
        // ngay dưới khóa PESSIMISTIC_WRITE của Booking, và không nuốt ngoại lệ hạ tầng làm hỏng rollback-only state.
        ConfirmBookingUseCase.ConfirmationOutcome outcome = confirmBookingUseCase.confirmWithOutcome(bookingId, now);
        if (outcome == ConfirmBookingUseCase.ConfirmationOutcome.CONFIRMED) {
            return BookingConfirmResult.CONFIRMED;
        }
        if (outcome == ConfirmBookingUseCase.ConfirmationOutcome.ALREADY_CONFIRMED) {
            return BookingConfirmResult.ALREADY_CONFIRMED;
        }

        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking != null && booking.getStatus() == BookingStatus.CONFIRMED) {
            return BookingConfirmResult.ALREADY_CONFIRMED;
        }
        return BookingConfirmResult.EXPIRED_RECONCILIATION_REQUIRED;
    }
}
