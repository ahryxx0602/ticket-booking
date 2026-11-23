package com.ticket.booking.application.usecase;

import com.ticket.booking.application.dto.BookingResponse;
import com.ticket.shared.dto.PageResponse;

public interface GetMyBookingsUseCase {
    PageResponse<BookingResponse> execute(String authenticatedUsername, int page, int size);
}
