package com.ticket.catalog.presentation.controller;

import com.ticket.catalog.application.dto.CreateStationCommand;
import com.ticket.catalog.application.dto.CreateTripCommand;
import com.ticket.catalog.application.dto.StationDto;
import com.ticket.catalog.application.dto.TripDto;
import com.ticket.catalog.application.usecase.CancelTripUseCase;
import com.ticket.catalog.application.usecase.CreateStationUseCase;
import com.ticket.catalog.application.usecase.CreateTripUseCase;
import com.ticket.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCatalogController {

    private final CreateStationUseCase createStationUseCase;
    private final CreateTripUseCase createTripUseCase;
    private final CancelTripUseCase cancelTripUseCase;

    @PostMapping("/stations")
    public ApiResponse<StationDto> createStation(@Valid @RequestBody CreateStationCommand command) {
        StationDto result = createStationUseCase.execute(command);
        return ApiResponse.ok("Station created successfully", result);
    }

    @PostMapping("/trips")
    public ApiResponse<TripDto> createTrip(@Valid @RequestBody CreateTripCommand command) {
        TripDto result = createTripUseCase.execute(command);
        return ApiResponse.ok("Trip and seat matrix created successfully", result);
    }

    @PatchMapping("/trips/{id}/cancel")
    public ApiResponse<TripDto> cancelTrip(@PathVariable Long id) {
        TripDto result = cancelTripUseCase.execute(id);
        return ApiResponse.ok("Trip cancelled successfully", result);
    }
}
