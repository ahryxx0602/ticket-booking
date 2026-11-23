package com.ticket.catalog.application.usecase;

import com.ticket.catalog.application.dto.StationDto;

import java.util.List;

public interface GetStationsUseCase {
    List<StationDto> execute();
}
