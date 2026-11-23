package com.ticket.booking.application.usecase;

import com.ticket.booking.application.dto.BookingResponse;
import com.ticket.booking.domain.model.BookingStatus;
import com.ticket.shared.dto.PageResponse;

public interface AdminGetBookingsUseCase {
    PageResponse<BookingResponse> execute(BookingStatus status, Long tripId, Long userId, int page, int size);
}
