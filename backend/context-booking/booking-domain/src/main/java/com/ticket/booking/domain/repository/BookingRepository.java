package com.ticket.booking.domain.repository;

import com.ticket.booking.domain.model.Booking;
import com.ticket.booking.domain.model.BookingStatus;
import com.ticket.shared.dto.PageResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface BookingRepository {
    Optional<Booking> findById(Long id);
    default Optional<Booking> findByIdForUpdate(Long id) {
        return findById(id);
    }
    Optional<Booking> findByReference(String bookingReference);
    default Optional<Booking> findByUserIdAndClientRequestId(Long userId, String clientRequestId) {
        return Optional.empty();
    }
    Optional<Booking> findByReferenceForUpdate(String bookingReference);
    Set<String> findActiveBookedSeatNumbers(Long tripId, LocalDateTime now);
    default Set<String> findConfirmedSeatNumbers(Long tripId) {
        return java.util.Collections.emptySet();
    }
    default Set<String> findActiveHeldSeatNumbers(Long tripId, LocalDateTime now) {
        return java.util.Collections.emptySet();
    }
    PageResponse<Booking> findByUserId(Long userId, int page, int size);
    PageResponse<Booking> findAllWithFilters(BookingStatus status, Long tripId, Long userId, int page, int size);
    List<Booking> findExpiredHeldBookings(LocalDateTime now, int limit);
    Booking save(Booking booking);
}
