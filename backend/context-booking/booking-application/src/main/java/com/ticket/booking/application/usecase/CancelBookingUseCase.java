package com.ticket.booking.application.usecase;

public interface CancelBookingUseCase {
    void execute(String bookingReference, String authenticatedUsername);
}
