package com.ticket.notification.infrastructure.persistence.repository;

import com.ticket.notification.infrastructure.persistence.entity.NotificationJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SpringDataNotificationRepository extends JpaRepository<NotificationJpaEntity, Long> {
    Optional<NotificationJpaEntity> findByIdempotencyKey(String idempotencyKey);
    List<NotificationJpaEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    long countByUserId(Long userId);

    @Query("SELECT n FROM NotificationJpaEntity n WHERE n.status = 'FAILED' AND n.retryCount < :maxRetries ORDER BY n.createdAt ASC")
    List<NotificationJpaEntity> findFailedNotificationsForRetry(@Param("maxRetries") int maxRetries, Pageable pageable);
}
