package com.ticket.notification.application.usecase;

import com.ticket.shared.event.BookingExpiredEvent;

public interface SendBookingExpiredNotificationUseCase {
    void execute(BookingExpiredEvent event);
}
