package com.ticket.adapter;

import com.ticket.booking.domain.model.Booking;
import com.ticket.booking.domain.repository.BookingRepository;
import com.ticket.payment.domain.port.PaymentBookingPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PaymentBookingPortAdapter implements PaymentBookingPort {

    private final BookingRepository bookingRepository;

    @Override
    public Optional<BookingSnapshot> findBookingByReference(String bookingReference) {
        return bookingRepository.findByReference(bookingReference)
                .map(this::toSnapshot);
    }

    private BookingSnapshot toSnapshot(Booking b) {
        return new BookingSnapshot(
                b.getId(),
                b.getBookingReference(),
                b.getUserId(),
                b.getTotalAmount(),
                b.getStatus() != null ? b.getStatus().name() : null,
                b.getExpiresAt()
        );
    }
}
