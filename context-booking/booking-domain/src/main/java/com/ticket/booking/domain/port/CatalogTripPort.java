package com.ticket.booking.domain.port;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

public interface CatalogTripPort {

    Optional<TripSnapshot> findTripWithSeats(Long tripId);

    record TripSeatInfo(Long seatId, String seatNumber, BigDecimal price) {}

    record TripSnapshot(Long tripId, boolean availableForBooking, Map<String, TripSeatInfo> seatsByNumber) {}
}
