package com.ticket.catalog.domain.repository;

import com.ticket.catalog.domain.model.Trip;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TripRepository {
    Optional<Trip> findById(Long id);
    Optional<Trip> findByTripCode(String tripCode);
    List<Trip> searchTrips(String departureCity, String arrivalCity, LocalDateTime fromTime, LocalDateTime toTime);
    Trip save(Trip trip);
}
