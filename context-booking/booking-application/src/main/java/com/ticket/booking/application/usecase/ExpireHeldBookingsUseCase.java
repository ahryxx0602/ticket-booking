package com.ticket.booking.application.usecase;

import java.time.LocalDateTime;

public interface ExpireHeldBookingsUseCase {
    int execute(LocalDateTime now);
}
