package com.ticket.catalog.presentation.controller;

import com.ticket.catalog.application.dto.CreateStationCommand;
import com.ticket.catalog.application.dto.CreateTripCommand;
import com.ticket.catalog.application.dto.StationDto;
import com.ticket.catalog.application.dto.TripDto;
import com.ticket.catalog.application.usecase.AdminManageCatalogUseCase;
import com.ticket.catalog.application.usecase.CancelTripUseCase;
import com.ticket.catalog.application.usecase.CreateStationUseCase;
import com.ticket.catalog.application.usecase.CreateTripUseCase;
import com.ticket.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCatalogController {

    private final CreateStationUseCase createStationUseCase;
    private final CreateTripUseCase createTripUseCase;
    private final CancelTripUseCase cancelTripUseCase;

    private AdminManageCatalogUseCase manageCatalogUseCase;

    @Autowired(required = false)
    public void setManageCatalogUseCase(AdminManageCatalogUseCase manageCatalogUseCase) {
        this.manageCatalogUseCase = manageCatalogUseCase;
    }

    @GetMapping("/stations")
    public ApiResponse<List<StationDto>> getStations() {
        List<StationDto> result = manageCatalogUseCase.getAllStations();
        return ApiResponse.ok("Stations retrieved successfully", result);
    }

    @PostMapping("/stations")
    public ApiResponse<StationDto> createStation(@Valid @RequestBody CreateStationCommand command) {
        StationDto result = createStationUseCase.execute(command);
        return ApiResponse.ok("Station created successfully", result);
    }

    @PutMapping("/stations/{id}")
    public ApiResponse<StationDto> updateStation(
            @PathVariable Long id,
            @Valid @RequestBody CreateStationCommand command) {
        StationDto result = manageCatalogUseCase.updateStation(id, command);
        return ApiResponse.ok("Station updated successfully", result);
    }

    @DeleteMapping("/stations/{id}")
    public ApiResponse<Void> deleteStation(@PathVariable Long id) {
        manageCatalogUseCase.deleteStation(id);
        return ApiResponse.ok("Station deleted successfully", null);
    }

    @GetMapping("/trips")
    public ApiResponse<List<TripDto>> getTrips() {
        List<TripDto> result = manageCatalogUseCase.getAllTrips();
        return ApiResponse.ok("Trips retrieved successfully", result);
    }

    @PostMapping("/trips")
    public ApiResponse<TripDto> createTrip(@Valid @RequestBody CreateTripCommand command) {
        TripDto result = createTripUseCase.execute(command);
        return ApiResponse.ok("Trip and seat matrix created successfully", result);
    }

    @PutMapping("/trips/{id}")
    public ApiResponse<TripDto> updateTrip(
            @PathVariable Long id,
            @Valid @RequestBody CreateTripCommand command) {
        TripDto result = manageCatalogUseCase.updateTrip(id, command);
        return ApiResponse.ok("Trip updated successfully", result);
    }

    @PatchMapping("/trips/{id}/cancel")
    public ApiResponse<TripDto> cancelTrip(@PathVariable Long id) {
        TripDto result = cancelTripUseCase.execute(id);
        return ApiResponse.ok("Trip cancelled successfully", result);
    }

    @DeleteMapping("/trips/{id}")
    public ApiResponse<Void> deleteTrip(@PathVariable Long id) {
        manageCatalogUseCase.deleteTrip(id);
        return ApiResponse.ok("Trip deleted successfully", null);
    }
}
