package com.ticket.catalog.application.usecase;

import com.ticket.catalog.application.dto.CreateTripCommand;
import com.ticket.catalog.application.dto.TripDto;

public interface CreateTripUseCase {
    TripDto execute(CreateTripCommand command);
}
