package com.ticket.catalog.infrastructure.persistence.adapter;

import com.ticket.catalog.domain.model.Seat;
import com.ticket.catalog.domain.model.Station;
import com.ticket.catalog.domain.model.Trip;
import com.ticket.catalog.domain.model.TripStatus;
import com.ticket.catalog.domain.repository.TripRepository;
import com.ticket.catalog.infrastructure.persistence.entity.SeatJpaEntity;
import com.ticket.catalog.infrastructure.persistence.entity.TripJpaEntity;
import com.ticket.catalog.infrastructure.persistence.repository.SpringDataSeatRepository;
import com.ticket.catalog.infrastructure.persistence.repository.SpringDataStationRepository;
import com.ticket.catalog.infrastructure.persistence.repository.SpringDataTripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TripRepositoryImpl implements TripRepository {

    private final SpringDataTripRepository springDataTripRepository;
    private final SpringDataStationRepository springDataStationRepository;
    private final SpringDataSeatRepository springDataSeatRepository;

    @Override
    public Optional<Trip> findById(Long id) {
        return springDataTripRepository.findByIdWithStations(id).map(this::toDomain);
    }

    @Override
    public Optional<Trip> findByTripCode(String tripCode) {
        return springDataTripRepository.findByTripCodeWithStations(tripCode).map(this::toDomain);
    }

    @Override
    public boolean existsByTripCode(String tripCode) {
        return springDataTripRepository.existsByTripCode(tripCode);
    }

    @Override
    public List<Trip> findAll() {
        return springDataTripRepository.findAllWithStations()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Trip> searchTrips(String departureCity, String arrivalCity, LocalDateTime fromTime, LocalDateTime toTime) {
        return springDataTripRepository.searchTrips(departureCity, arrivalCity, fromTime, toTime)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Seat> findSeatsByTripId(Long tripId) {
        return springDataSeatRepository.findByTripIdOrderBySeatNumberAsc(tripId)
                .stream()
                .map(this::toSeatDomain)
                .toList();
    }

    @Override
    public List<Seat> lockSeatsByTripIdAndNumbers(Long tripId, List<String> seatNumbers) {
        if (seatNumbers == null || seatNumbers.isEmpty()) {
            return Collections.emptyList();
        }
        return springDataSeatRepository.findAndLockSeatsByTripIdAndNumbers(tripId, seatNumbers)
                .stream()
                .map(this::toSeatDomain)
                .toList();
    }

    @Override
    public Trip save(Trip trip) {
        TripJpaEntity entity = new TripJpaEntity();
        entity.setId(trip.getId());
        entity.setTripCode(trip.getTripCode());
        entity.setDepartureStation(springDataStationRepository.getReferenceById(trip.getDepartureStation().getId()));
        entity.setArrivalStation(springDataStationRepository.getReferenceById(trip.getArrivalStation().getId()));
        entity.setDepartureTime(trip.getDepartureTime());
        entity.setArrivalTime(trip.getArrivalTime());
        entity.setTransportType(trip.getTransportType());
        entity.setTotalSeats(trip.getTotalSeats());
        entity.setBasePrice(trip.getBasePrice());
        entity.setStatus(trip.getStatus() != null ? trip.getStatus() : TripStatus.SCHEDULED);
        entity.setOperatorId(trip.getOperatorId());
        entity.setVehicleId(trip.getVehicleId());
        entity.setSettlementStatus(trip.getSettlementStatus() != null ? trip.getSettlementStatus() : "UNSETTLED");

        TripJpaEntity savedEntity = springDataTripRepository.save(entity);

        if (trip.getSeats() != null && !trip.getSeats().isEmpty()
                && !springDataSeatRepository.existsByTripId(savedEntity.getId())) {
            List<SeatJpaEntity> seatEntities = trip.getSeats().stream()
                    .map(seat -> {
                        SeatJpaEntity se = new SeatJpaEntity();
                        se.setTripId(savedEntity.getId());
                        se.setSeatNumber(seat.getSeatNumber());
                        se.setSeatType(seat.getSeatType());
                        se.setPriceMultiplier(seat.getPriceMultiplier());
                        return se;
                    })
                    .toList();
            springDataSeatRepository.saveAll(seatEntities);
        }

        Trip savedDomain = Trip.builder()
                .id(savedEntity.getId())
                .tripCode(savedEntity.getTripCode())
                .departureStation(trip.getDepartureStation())
                .arrivalStation(trip.getArrivalStation())
                .departureTime(savedEntity.getDepartureTime())
                .arrivalTime(savedEntity.getArrivalTime())
                .transportType(savedEntity.getTransportType())
                .totalSeats(savedEntity.getTotalSeats())
                .basePrice(savedEntity.getBasePrice())
                .status(savedEntity.getStatus())
                .operatorId(savedEntity.getOperatorId())
                .vehicleId(savedEntity.getVehicleId())
                .settlementStatus(savedEntity.getSettlementStatus())
                .seats(trip.getSeats() != null ? trip.getSeats() : Collections.emptyList())
                .build();
        savedDomain.setCreatedAt(savedEntity.getCreatedAt());
        savedDomain.setUpdatedAt(savedEntity.getUpdatedAt());
        return savedDomain;
    }

    @Override
    public void deleteById(Long id) {
        springDataTripRepository.deleteById(id);
        springDataTripRepository.flush();
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
                .status(entity.getStatus() != null ? entity.getStatus() : TripStatus.SCHEDULED)
                .operatorId(entity.getOperatorId())
                .vehicleId(entity.getVehicleId())
                .settlementStatus(entity.getSettlementStatus() != null ? entity.getSettlementStatus() : "UNSETTLED")
                .seats(Collections.emptyList())
                .build();
        trip.setCreatedAt(entity.getCreatedAt());
        trip.setUpdatedAt(entity.getUpdatedAt());
        return trip;
    }

    private Seat toSeatDomain(SeatJpaEntity entity) {
        return Seat.builder()
                .id(entity.getId())
                .tripId(entity.getTripId())
                .seatNumber(entity.getSeatNumber())
                .seatType(entity.getSeatType())
                .priceMultiplier(entity.getPriceMultiplier())
                .build();
    }
}
