package com.ticket.adapter;

import com.ticket.booking.domain.port.CatalogTripPort;
import com.ticket.catalog.domain.model.Seat;
import com.ticket.catalog.domain.model.Trip;
import com.ticket.catalog.domain.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CatalogTripPortAdapter implements CatalogTripPort {

    private final TripRepository tripRepository;

    @Override
    public Optional<TripSnapshot> findTripWithSeats(Long tripId) {
        return tripRepository.findById(tripId).map(trip -> toSnapshot(trip, tripRepository.findSeatsByTripId(tripId)));
    }

    private TripSnapshot toSnapshot(Trip trip, List<Seat> seats) {
        Map<String, TripSeatInfo> seatMap = seats.stream()
                .collect(Collectors.toMap(
                        seat -> seat.getSeatNumber().trim().toUpperCase(),
                        seat -> new TripSeatInfo(
                                seat.getId(),
                                seat.getSeatNumber().trim().toUpperCase(),
                                seat.calculatePrice(trip.getBasePrice())
                        ),
                        (first, second) -> first
                ));
        return new TripSnapshot(trip.getId(), trip.isAvailableForBooking(), seatMap);
    }
}
