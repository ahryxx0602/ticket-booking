package com.ticket.catalog.application.usecase;

import com.ticket.catalog.application.dto.CreateStationCommand;
import com.ticket.catalog.application.dto.CreateTripCommand;
import com.ticket.catalog.application.dto.StationDto;
import com.ticket.catalog.application.dto.TripDto;

import java.util.List;

public interface AdminManageCatalogUseCase {
    List<StationDto> getAllStations();
    StationDto updateStation(Long id, CreateStationCommand command);
    void deleteStation(Long id);

    List<TripDto> getAllTrips();
    TripDto updateTrip(Long id, CreateTripCommand command);
    void deleteTrip(Long id);
}
