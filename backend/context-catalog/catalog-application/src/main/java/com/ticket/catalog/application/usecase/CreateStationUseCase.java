package com.ticket.catalog.application.usecase;

import com.ticket.catalog.application.dto.CreateStationCommand;
import com.ticket.catalog.application.dto.StationDto;

public interface CreateStationUseCase {
    StationDto execute(CreateStationCommand command);
}
