package com.ticket.booking.application.usecase;

import com.ticket.booking.application.dto.SeatAvailabilityResponse;

public interface GetSeatAvailabilityUseCase {
    SeatAvailabilityResponse execute(Long tripId);
}
