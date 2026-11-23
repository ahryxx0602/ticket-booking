package com.ticket.notification.application.usecase;

import com.ticket.shared.event.ReconciliationAlertEvent;

public interface SendAdminAlertUseCase {
    void execute(ReconciliationAlertEvent event);
}
