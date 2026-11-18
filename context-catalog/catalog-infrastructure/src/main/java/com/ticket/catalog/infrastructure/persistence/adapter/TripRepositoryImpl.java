package com.ticket.catalog.infrastructure.persistence.adapter;

import com.ticket.catalog.domain.model.Station;
import com.ticket.catalog.domain.model.Trip;
import com.ticket.catalog.domain.model.TripStatus;
import com.ticket.catalog.domain.repository.TripRepository;
import com.ticket.catalog.infrastructure.persistence.entity.TripJpaEntity;
import com.ticket.catalog.infrastructure.persistence.repository.SpringDataTripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TripRepositoryImpl implements TripRepository {

    private final SpringDataTripRepository springDataTripRepository;

    @Override
    public Optional<Trip> findById(Long id) {
        return springDataTripRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Trip> findByTripCode(String tripCode) {
        return springDataTripRepository.findByTripCode(tripCode).map(this::toDomain);
    }

    @Override
    public List<Trip> searchTrips(String departureCity, String arrivalCity, LocalDateTime fromTime, LocalDateTime toTime) {
        return springDataTripRepository.searchTrips(departureCity, arrivalCity, fromTime, toTime)
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public Trip save(Trip trip) {
        // Mock persistence return for skeleton
        return trip;
    }

    private Trip toDomain(TripJpaEntity entity) {
        Trip trip = Trip.builder()
                .id(entity.getId())
                .tripCode(entity.getTripCode())
                .departureStation(Station.builder()
                        .id(entity.getDepartureStation().getId())
                        .code(entity.getDepartureStation().getCode())
                        .name(entity.getDepartureStation().getName())
                        .city(entity.getDepartureStation().getCity())
                        .build())
                .arrivalStation(Station.builder()
                        .id(entity.getArrivalStation().getId())
                        .code(entity.getArrivalStation().getCode())
                        .name(entity.getArrivalStation().getName())
                        .city(entity.getArrivalStation().getCity())
                        .build())
                .departureTime(entity.getDepartureTime())
                .arrivalTime(entity.getArrivalTime())
                .transportType(entity.getTransportType())
                .totalSeats(entity.getTotalSeats())
                .basePrice(entity.getBasePrice())
                .status(TripStatus.fromString(entity.getStatus()))
                .seats(Collections.emptyList())
                .build();
        trip.setCreatedAt(entity.getCreatedAt());
        trip.setUpdatedAt(entity.getUpdatedAt());
        return trip;
    }
}
