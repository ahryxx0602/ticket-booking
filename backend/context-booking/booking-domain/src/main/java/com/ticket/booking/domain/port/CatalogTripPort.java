package com.ticket.booking.domain.port;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface CatalogTripPort {

    Optional<TripSnapshot> findTripWithSeats(Long tripId);

    /**
     * BE-CON-001: Khóa bi quan (PESSIMISTIC_WRITE) các dòng ghế theo thứ tự tăng dần trong MySQL
     * nhằm chống Double-Booking 100% ở tầng Database ngay cả khi Redis bị evict hoặc failover.
     */
    default void lockSeatsForBooking(Long tripId, List<String> sortedSeatNumbers) {
    }

    record TripSeatInfo(Long seatId, String seatNumber, BigDecimal price) {}

    record TripSnapshot(Long tripId, boolean availableForBooking, Map<String, TripSeatInfo> seatsByNumber) {}
}
