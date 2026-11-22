package com.ticket.adapter;

import com.ticket.catalog.domain.model.Station;
import com.ticket.catalog.domain.model.Trip;
import com.ticket.catalog.domain.repository.TripRepository;
import com.ticket.notification.domain.port.NotificationCatalogPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class NotificationCatalogPortAdapter implements NotificationCatalogPort {

    private final TripRepository tripRepository;

    @Override
    public Optional<TripDetailsSnapshot> getTripDetails(Long tripId) {
        if (tripId == null) {
            return Optional.empty();
        }
        return tripRepository.findById(tripId).map(this::toSnapshot);
    }

    private TripDetailsSnapshot toSnapshot(Trip trip) {
        String depStationName = Optional.ofNullable(trip.getDepartureStation())
                .map(Station::getName)
                .orElse("Unknown Departure");
        String arrStationName = Optional.ofNullable(trip.getArrivalStation())
                .map(Station::getName)
                .orElse("Unknown Arrival");

        return new TripDetailsSnapshot(
                trip.getId(),
                trip.getTripCode(),
                depStationName,
                arrStationName,
                trip.getDepartureTime(),
                trip.getArrivalTime(),
                null // operatorId reserved for Phase 6 multi-tenant
        );
    }
}
