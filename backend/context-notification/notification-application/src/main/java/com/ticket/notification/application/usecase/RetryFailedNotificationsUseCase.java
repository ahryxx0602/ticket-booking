package com.ticket.notification.application.usecase;

public interface RetryFailedNotificationsUseCase {
    int execute(int maxRetries, int batchSize);
}
