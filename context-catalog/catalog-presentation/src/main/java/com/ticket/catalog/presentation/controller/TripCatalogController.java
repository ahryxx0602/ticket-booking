package com.ticket.catalog.presentation.controller;

import com.ticket.catalog.application.dto.SearchTripQuery;
import com.ticket.catalog.application.dto.SeatDto;
import com.ticket.catalog.application.dto.StationDto;
import com.ticket.catalog.application.dto.TripDto;
import com.ticket.catalog.application.usecase.GetStationsUseCase;
import com.ticket.catalog.application.usecase.GetTripSeatsUseCase;
import com.ticket.catalog.application.usecase.SearchTripsUseCase;
import com.ticket.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/catalog")
@RequiredArgsConstructor
public class TripCatalogController {

    private final SearchTripsUseCase searchTripsUseCase;
    private final GetStationsUseCase getStationsUseCase;
    private final GetTripSeatsUseCase getTripSeatsUseCase;

    @GetMapping("/trips")
    public ResponseEntity<ApiResponse<List<TripDto>>> searchTrips(
            @RequestParam(required = false) String departureCity,
            @RequestParam(required = false) String arrivalCity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate departureDate
    ) {
        SearchTripQuery query = new SearchTripQuery();
        query.setDepartureCity(departureCity);
        query.setArrivalCity(arrivalCity);
        query.setDepartureDate(departureDate);

        List<TripDto> result = searchTripsUseCase.execute(query);
        return ResponseEntity.ok(ApiResponse.ok("Trips retrieved successfully", result));
    }

    @GetMapping("/stations")
    public ResponseEntity<ApiResponse<List<StationDto>>> getStations() {
        List<StationDto> result = getStationsUseCase.execute();
        return ResponseEntity.ok(ApiResponse.ok("Stations retrieved successfully", result));
    }

    @GetMapping("/trips/{id}/seats")
    public ResponseEntity<ApiResponse<List<SeatDto>>> getTripSeats(@PathVariable Long id) {
        List<SeatDto> result = getTripSeatsUseCase.execute(id);
        return ResponseEntity.ok(ApiResponse.ok("Trip seat matrix retrieved successfully", result));
    }
}
