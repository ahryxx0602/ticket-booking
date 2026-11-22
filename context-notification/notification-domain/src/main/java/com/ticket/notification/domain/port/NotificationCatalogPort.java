package com.ticket.notification.domain.port;

import java.time.LocalDateTime;
import java.util.Optional;

public interface NotificationCatalogPort {
    record TripDetailsSnapshot(
            Long tripId,
            String tripCode,
            String departureStationName,
            String arrivalStationName,
            LocalDateTime departureTime,
            LocalDateTime arrivalTime,
            Long operatorId
    ) {}

    Optional<TripDetailsSnapshot> getTripDetails(Long tripId);
}
