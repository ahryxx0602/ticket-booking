package com.ticket.notification.application.usecase;

import com.ticket.shared.event.PaymentConfirmedEvent;

public interface SendPaymentNotificationUseCase {
    void execute(PaymentConfirmedEvent event);
}
