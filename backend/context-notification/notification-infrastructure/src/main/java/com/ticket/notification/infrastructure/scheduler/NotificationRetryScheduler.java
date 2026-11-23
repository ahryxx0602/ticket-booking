package com.ticket.notification.infrastructure.scheduler;

import com.ticket.notification.application.usecase.RetryFailedNotificationsUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.notification.retry.enabled", havingValue = "true", matchIfMissing = true)
public class NotificationRetryScheduler {

    private final RetryFailedNotificationsUseCase retryFailedNotificationsUseCase;

    @Scheduled(fixedDelay = 60000)
    public void retryFailedNotifications() {
        try {
            int retried = retryFailedNotificationsUseCase.execute(3, 20);
            if (retried > 0) {
                log.info("[NOTIFICATION RETRY] Successfully retried {} failed notifications", retried);
            }
        } catch (Exception ex) {
            log.error("[NOTIFICATION RETRY ERROR] Exception occurred during retry scan", ex);
        }
    }
}
