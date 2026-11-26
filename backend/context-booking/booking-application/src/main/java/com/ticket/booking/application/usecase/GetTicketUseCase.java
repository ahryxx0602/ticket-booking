package com.ticket.booking.application.usecase;

import com.ticket.booking.application.dto.TicketResponse;

public interface GetTicketUseCase {
    TicketResponse getTicket(String bookingReference, String authenticatedUsername);
}
