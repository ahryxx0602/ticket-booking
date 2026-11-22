package com.ticket.notification.domain.port;

import com.ticket.notification.domain.model.Notification;
import com.ticket.notification.domain.model.NotificationStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository {
    Notification save(Notification notification);
    Optional<Notification> findById(Long id);
    Optional<Notification> findByIdempotencyKey(String idempotencyKey);
    List<Notification> findByUserId(Long userId, int offset, int limit);
    long countByUserId(Long userId);
    List<Notification> findFailedNotificationsForRetry(int maxRetries, LocalDateTime before, int limit);
}
