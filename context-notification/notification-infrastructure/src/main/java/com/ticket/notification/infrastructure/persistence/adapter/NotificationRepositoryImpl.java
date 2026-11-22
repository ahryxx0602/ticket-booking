package com.ticket.notification.infrastructure.persistence.adapter;

import com.ticket.notification.domain.model.Notification;
import com.ticket.notification.domain.port.NotificationRepository;
import com.ticket.notification.infrastructure.persistence.entity.NotificationJpaEntity;
import com.ticket.notification.infrastructure.persistence.repository.SpringDataNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class NotificationRepositoryImpl implements NotificationRepository {

    private final SpringDataNotificationRepository jpaRepository;

    @Override
    public Notification save(Notification domain) {
        NotificationJpaEntity entity = toEntity(domain);
        NotificationJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Notification> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Notification> findByIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null) return Optional.empty();
        return jpaRepository.findByIdempotencyKey(idempotencyKey).map(this::toDomain);
    }

    @Override
    public List<Notification> findByUserId(Long userId, int offset, int limit) {
        int page = offset / Math.max(limit, 1);
        return jpaRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, limit)).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long countByUserId(Long userId) {
        return jpaRepository.countByUserId(userId);
    }

    @Override
    public List<Notification> findFailedNotificationsForRetry(int maxRetries, LocalDateTime before, int limit) {
        return jpaRepository.findFailedNotificationsForRetry(maxRetries, PageRequest.of(0, limit)).stream()
                .map(this::toDomain)
                .toList();
    }

    private Notification toDomain(NotificationJpaEntity entity) {
        Notification n = Notification.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .recipient(entity.getRecipient())
                .channel(entity.getChannel())
                .type(entity.getType())
                .status(entity.getStatus())
                .subject(entity.getSubject())
                .content(entity.getContent())
                .metadataJson(entity.getMetadataJson())
                .idempotencyKey(entity.getIdempotencyKey())
                .errorMessage(entity.getErrorMessage())
                .sentAt(entity.getSentAt())
                .retryCount(entity.getRetryCount())
                .build();
        n.setCreatedAt(entity.getCreatedAt());
        return n;
    }

    private NotificationJpaEntity toEntity(Notification domain) {
        NotificationJpaEntity entity = new NotificationJpaEntity();
        entity.setId(domain.getId());
        entity.setUserId(domain.getUserId());
        entity.setRecipient(domain.getRecipient());
        entity.setChannel(domain.getChannel());
        entity.setType(domain.getType());
        entity.setStatus(domain.getStatus());
        entity.setSubject(domain.getSubject());
        entity.setContent(domain.getContent());
        entity.setMetadataJson(domain.getMetadataJson());
        entity.setIdempotencyKey(domain.getIdempotencyKey());
        entity.setErrorMessage(domain.getErrorMessage());
        entity.setSentAt(domain.getSentAt());
        entity.setRetryCount(domain.getRetryCount());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }
}
