package com.ticket.adapter;

import com.ticket.booking.domain.model.Booking;
import com.ticket.booking.domain.repository.BookingRepository;
import com.ticket.identity.domain.model.User;
import com.ticket.identity.domain.repository.UserRepository;
import com.ticket.payment.domain.port.PaymentBookingPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PaymentBookingPortAdapter implements PaymentBookingPort {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    @Override
    public Optional<BookingSnapshot> findBookingByReference(String bookingReference) {
        return bookingRepository.findByReference(bookingReference)
                .map(this::toSnapshot);
    }

    @Override
    public Optional<BookingSnapshot> findBookingById(Long bookingId) {
        if (bookingId == null) {
            return Optional.empty();
        }
        return bookingRepository.findById(bookingId)
                .map(this::toSnapshot);
    }

    private BookingSnapshot toSnapshot(Booking b) {
        String email = null;
        if (b.getUserId() != null) {
            email = userRepository.findById(b.getUserId()).map(User::getEmail).orElse(null);
        }

        return new BookingSnapshot(
                b.getId(),
                b.getBookingReference(),
                b.getUserId(),
                b.getTotalAmount(),
                b.getStatus() != null ? b.getStatus().name() : null,
                b.getExpiresAt(),
                b.getTripId(),
                b.getPhone(),
                email,
                b.getSeatNumbers()
        );
    }
}
