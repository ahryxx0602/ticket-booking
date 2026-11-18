package com.ticket.catalog.infrastructure.service;

import com.ticket.catalog.application.dto.SearchTripQuery;
import com.ticket.catalog.application.dto.TripDto;
import com.ticket.catalog.application.usecase.SearchTripsUseCase;
import com.ticket.catalog.domain.model.Trip;
import com.ticket.catalog.domain.repository.TripRepository;
import com.ticket.catalog.infrastructure.cache.TripRedisCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchTripsUseCaseImpl implements SearchTripsUseCase {

    private final TripRepository tripRepository;
    private final TripRedisCacheService cacheService;

    @Override
    public List<TripDto> execute(SearchTripQuery query) {
        String cacheKey = String.format("%s_%s_%s", query.getDepartureCity(), query.getArrivalCity(), query.getDepartureDate());
        
        // 1. Check Redis Cache
        List<TripDto> cached = cacheService.getCachedSearch(cacheKey);
        if (!cached.isEmpty()) {
            return cached;
        }

        // 2. Query MySQL
        LocalDateTime fromTime = query.getDepartureDate() != null ? query.getDepartureDate().atStartOfDay() : LocalDateTime.now();
        LocalDateTime toTime = query.getDepartureDate() != null ? query.getDepartureDate().atTime(LocalTime.MAX) : fromTime.plusDays(7);

        List<Trip> trips = tripRepository.searchTrips(query.getDepartureCity(), query.getArrivalCity(), fromTime, toTime);

        List<TripDto> result = trips.stream().map(t -> TripDto.builder()
                .id(t.getId())
                .tripCode(t.getTripCode())
                .departureStationName(t.getDepartureStation().getName())
                .arrivalStationName(t.getArrivalStation().getName())
                .departureTime(t.getDepartureTime())
                .arrivalTime(t.getArrivalTime())
                .transportType(t.getTransportType().name())
                .basePrice(t.getBasePrice())
                .totalSeats(t.getTotalSeats())
                .build()
        ).collect(Collectors.toList());

        // 3. Populate Redis Cache with 10 minutes TTL
        if (!result.isEmpty()) {
            cacheService.setCachedSearch(cacheKey, result, Duration.ofMinutes(10));
        }

        return result;
    }
}
