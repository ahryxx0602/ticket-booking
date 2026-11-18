package com.ticket.booking.domain.repository;

import com.ticket.booking.domain.model.Booking;
import java.util.Optional;

public interface BookingRepository {
    Optional<Booking> findById(Long id);
    Optional<Booking> findByReference(String bookingReference);
    Booking save(Booking booking);
}
