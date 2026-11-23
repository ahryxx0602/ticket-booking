package com.ticket.catalog.application.usecase;

import com.ticket.catalog.application.dto.TripDto;

public interface CancelTripUseCase {
    TripDto execute(Long tripId);
}
