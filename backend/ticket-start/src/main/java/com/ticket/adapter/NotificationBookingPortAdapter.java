package com.ticket.adapter;

import com.ticket.booking.domain.model.Booking;
import com.ticket.booking.domain.repository.BookingRepository;
import com.ticket.identity.domain.repository.UserRepository;
import com.ticket.notification.domain.port.NotificationBookingPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class NotificationBookingPortAdapter implements NotificationBookingPort {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    @Override
    public Optional<BookingDetailsSnapshot> getBookingDetails(String bookingReference) {
        if (bookingReference == null || bookingReference.isBlank()) {
            return Optional.empty();
        }
        return bookingRepository.findByReference(bookingReference).map(this::toSnapshot);
    }

    @Override
    public Optional<BookingDetailsSnapshot> getBookingDetailsByReferenceAndPhone(String bookingReference, String phone) {
        if (bookingReference == null || bookingReference.isBlank() || phone == null || phone.isBlank()) {
            return Optional.empty();
        }
        String normalizedPhone = phone.trim();
        return bookingRepository.findByReference(bookingReference)
                .filter(b -> {
                    if (b.getPhone() != null && !b.getPhone().isBlank()) {
                        return normalizedPhone.equals(b.getPhone().trim());
                    }
                    if (b.getUserId() != null) {
                        return userRepository.findById(b.getUserId())
                                .map(u -> normalizedPhone.equals(u.getPhone()))
                                .orElse(false);
                    }
                    return false;
                })
                .map(this::toSnapshot);
    }

    private BookingDetailsSnapshot toSnapshot(Booking b) {
        return new BookingDetailsSnapshot(
                b.getId(),
                b.getBookingReference(),
                b.getUserId(),
                b.getTripId(),
                b.getStatus() != null ? b.getStatus().name() : null,
                b.getTotalAmount(),
                b.getPhone(),
                b.getExpiresAt(),
                b.getSeatNumbers()
        );
    }
}
