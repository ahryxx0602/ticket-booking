package com.ticket.booking.application.usecase;

import com.ticket.booking.application.dto.BookingResponse;

public interface GetBookingDetailUseCase {
    BookingResponse execute(String bookingReference, String authenticatedUsername);
}
