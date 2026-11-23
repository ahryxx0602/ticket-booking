package com.ticket.catalog.infrastructure.service;

import com.ticket.catalog.application.dto.SearchTripQuery;
import com.ticket.catalog.application.dto.SeatDto;
import com.ticket.catalog.application.dto.StationDto;
import com.ticket.catalog.application.dto.TripDto;
import com.ticket.catalog.application.usecase.GetStationsUseCase;
import com.ticket.catalog.application.usecase.GetTripSeatsUseCase;
import com.ticket.catalog.application.usecase.SearchTripsUseCase;
import com.ticket.catalog.domain.model.Seat;
import com.ticket.catalog.domain.model.Station;
import com.ticket.catalog.domain.model.Trip;
import com.ticket.catalog.domain.repository.StationRepository;
import com.ticket.catalog.domain.repository.TripRepository;
import com.ticket.catalog.infrastructure.cache.TripRedisCacheService;
import com.ticket.shared.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchTripsUseCaseImpl implements
        SearchTripsUseCase,
        GetStationsUseCase,
        GetTripSeatsUseCase {

    private final TripRepository tripRepository;
    private final StationRepository stationRepository;
    private final TripRedisCacheService cacheService;

    @Override
    @Transactional(readOnly = true)
    public List<TripDto> execute(SearchTripQuery query) {
        String depCity = normalizeBlankToNull(query.getDepartureCity());
        String arrCity = normalizeBlankToNull(query.getArrivalCity());
        String cacheKey = String.format("%s_%s_%s", depCity, arrCity, query.getDepartureDate());

        // 1. Check Redis Cache
        List<TripDto> cached = cacheService.getCachedSearch(cacheKey);
        if (!cached.isEmpty()) {
            return cached;
        }

        // 2. Query MySQL
        LocalDateTime fromTime = query.getDepartureDate() != null ? query.getDepartureDate().atStartOfDay() : LocalDateTime.now();
        LocalDateTime toTime = query.getDepartureDate() != null ? query.getDepartureDate().atTime(LocalTime.MAX) : fromTime.plusDays(7);

        List<Trip> trips = tripRepository.searchTrips(depCity, arrCity, fromTime, toTime);

        List<TripDto> result = trips.stream()
                .map(this::toTripDto)
                .toList();

        // 3. Populate Redis Cache with 10 minutes TTL
        if (!result.isEmpty()) {
            cacheService.setCachedSearch(cacheKey, result, Duration.ofMinutes(10));
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StationDto> execute() {
        return stationRepository.findAll()
                .stream()
                .map(this::toStationDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeatDto> execute(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new EntityNotFoundException("Trip", tripId));

        List<Seat> seats = tripRepository.findSeatsByTripId(tripId);
        return seats.stream()
                .map(seat -> SeatDto.builder()
                        .id(seat.getId())
                        .tripId(tripId)
                        .seatNumber(seat.getSeatNumber())
                        .seatType(seat.getSeatType() != null ? seat.getSeatType().name() : "STANDARD")
                        .priceMultiplier(seat.getPriceMultiplier())
                        .finalPrice(seat.calculatePrice(trip.getBasePrice()))
                        .build())
                .toList();
    }

    private String normalizeBlankToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private StationDto toStationDto(Station station) {
        return StationDto.builder()
                .id(station.getId())
                .code(station.getCode())
                .name(station.getName())
                .city(station.getCity())
                .build();
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
