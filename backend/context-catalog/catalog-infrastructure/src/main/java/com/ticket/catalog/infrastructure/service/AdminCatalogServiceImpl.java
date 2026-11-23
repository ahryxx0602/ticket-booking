package com.ticket.catalog.infrastructure.service;

import com.ticket.catalog.application.dto.CreateStationCommand;
import com.ticket.catalog.application.dto.CreateTripCommand;
import com.ticket.catalog.application.dto.StationDto;
import com.ticket.catalog.application.dto.TripDto;
import com.ticket.catalog.application.usecase.CancelTripUseCase;
import com.ticket.catalog.application.usecase.CreateStationUseCase;
import com.ticket.catalog.application.usecase.CreateTripUseCase;
import com.ticket.catalog.domain.model.Station;
import com.ticket.catalog.domain.model.Trip;
import com.ticket.catalog.domain.model.TripStatus;
import com.ticket.catalog.domain.repository.StationRepository;
import com.ticket.catalog.domain.repository.TripRepository;
import com.ticket.catalog.infrastructure.cache.TripRedisCacheService;
import com.ticket.shared.exception.BusinessException;
import com.ticket.shared.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCatalogServiceImpl implements
        CreateStationUseCase,
        CreateTripUseCase,
        CancelTripUseCase {

    private final StationRepository stationRepository;
    private final TripRepository tripRepository;
    private final TripRedisCacheService cacheService;

    @Override
    @Transactional
    public StationDto execute(CreateStationCommand command) {
        String normalizedCode = command.getCode().trim().toUpperCase();
        if (stationRepository.existsByCode(normalizedCode)) {
            throw new BusinessException("STATION_CODE_EXISTS", "Station code already exists: " + normalizedCode);
        }

        Station station = Station.builder()
                .code(normalizedCode)
                .name(command.getName().trim())
                .city(command.getCity().trim())
                .build();

        Station saved = stationRepository.save(station);
        log.info("[ADMIN CATALOG] Created station: {} ({})", saved.getCode(), saved.getName());
        return StationDto.builder()
                .id(saved.getId())
                .code(saved.getCode())
                .name(saved.getName())
                .city(saved.getCity())
                .build();
    }

    @Override
    @Transactional
    public TripDto execute(CreateTripCommand command) {
        String normalizedTripCode = command.getTripCode().trim().toUpperCase();
        if (tripRepository.existsByTripCode(normalizedTripCode)) {
            throw new BusinessException("TRIP_CODE_EXISTS", "Trip code already exists: " + normalizedTripCode);
        }

        Station departureStation = stationRepository.findById(command.getDepartureStationId())
                .orElseThrow(() -> new EntityNotFoundException("Station", command.getDepartureStationId()));
        Station arrivalStation = stationRepository.findById(command.getArrivalStationId())
                .orElseThrow(() -> new EntityNotFoundException("Station", command.getArrivalStationId()));

        Trip trip = Trip.builder()
                .tripCode(normalizedTripCode)
                .departureStation(departureStation)
                .arrivalStation(arrivalStation)
                .departureTime(command.getDepartureTime())
                .arrivalTime(command.getArrivalTime())
                .transportType(command.getTransportType())
                .basePrice(command.getBasePrice())
                .status(TripStatus.SCHEDULED)
                .build();

        trip.validateScheduleAndRoute();
        trip.generateSeatMatrix(command.getTotalRows(), command.getSeatsPerRow(), command.getVipRows());

        Trip saved = tripRepository.save(trip);
        triggerPostCommitCacheEviction();
        log.info("[ADMIN CATALOG] Created trip {} with {} seats ({} VIP rows)",
                saved.getTripCode(), saved.getTotalSeats(), command.getVipRows());

        return toTripDto(saved);
    }

    @Override
    @Transactional
    public TripDto execute(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new EntityNotFoundException("Trip", tripId));

        trip.cancel();
        Trip saved = tripRepository.save(trip);
        triggerPostCommitCacheEviction();
        log.info("[ADMIN CATALOG] Cancelled trip {} (id={})", saved.getTripCode(), saved.getId());

        return toTripDto(saved);
    }

    private void triggerPostCommitCacheEviction() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    cacheService.evictAllSearchCaches();
                }
            });
        } else {
            cacheService.evictAllSearchCaches();
        }
    }

    private TripDto toTripDto(Trip t) {
        return TripDto.builder()
                .id(t.getId())
                .tripCode(t.getTripCode())
                .departureStationCode(t.getDepartureStation().getCode())
                .departureStationName(t.getDepartureStation().getName())
                .departureCity(t.getDepartureStation().getCity())
                .arrivalStationCode(t.getArrivalStation().getCode())
                .arrivalStationName(t.getArrivalStation().getName())
                .arrivalCity(t.getArrivalStation().getCity())
                .departureTime(t.getDepartureTime())
                .arrivalTime(t.getArrivalTime())
                .transportType(t.getTransportType().name())
                .basePrice(t.getBasePrice())
                .totalSeats(t.getTotalSeats())
                .status(t.getStatus() != null ? t.getStatus().name() : "SCHEDULED")
                .build();
    }
}
