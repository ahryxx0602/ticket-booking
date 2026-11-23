package com.ticket.catalog.application.usecase;

import com.ticket.catalog.application.dto.SeatDto;

import java.util.List;

public interface GetTripSeatsUseCase {
    List<SeatDto> execute(Long tripId);
}
