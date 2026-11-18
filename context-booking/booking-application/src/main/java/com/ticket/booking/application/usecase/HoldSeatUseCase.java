package com.ticket.booking.application.usecase;

import com.ticket.booking.application.dto.BookingResponse;
import com.ticket.booking.application.dto.HoldSeatCommand;

public interface HoldSeatUseCase {
    BookingResponse execute(HoldSeatCommand command);
}
