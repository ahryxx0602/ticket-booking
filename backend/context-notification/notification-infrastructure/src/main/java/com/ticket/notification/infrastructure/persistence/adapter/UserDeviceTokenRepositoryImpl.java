package com.ticket.notification.infrastructure.persistence.adapter;

import com.ticket.notification.domain.model.UserDeviceToken;
import com.ticket.notification.domain.port.UserDeviceTokenRepository;
import com.ticket.notification.infrastructure.persistence.entity.UserDeviceTokenJpaEntity;
import com.ticket.notification.infrastructure.persistence.repository.SpringDataUserDeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserDeviceTokenRepositoryImpl implements UserDeviceTokenRepository {

    private final SpringDataUserDeviceTokenRepository jpaRepository;

    @Override
    public UserDeviceToken save(UserDeviceToken domain) {
        UserDeviceTokenJpaEntity entity = toEntity(domain);
        UserDeviceTokenJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<UserDeviceToken> findByFcmToken(String fcmToken) {
        return jpaRepository.findByFcmToken(fcmToken).map(this::toDomain);
    }

    @Override
    public List<UserDeviceToken> findByUserId(Long userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deleteByFcmTokenAndUserId(String fcmToken, Long userId) {
        jpaRepository.deleteByFcmTokenAndUserId(fcmToken, userId);
    }

    @Override
    public void deleteByFcmTokens(List<String> fcmTokens) {
        if (fcmTokens != null && !fcmTokens.isEmpty()) {
            jpaRepository.deleteByFcmTokenIn(fcmTokens);
        }
    }

    private UserDeviceToken toDomain(UserDeviceTokenJpaEntity entity) {
        UserDeviceToken domain = UserDeviceToken.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .fcmToken(entity.getFcmToken())
                .deviceType(entity.getDeviceType())
                .lastActiveAt(entity.getLastActiveAt())
                .build();
        domain.setCreatedAt(entity.getCreatedAt());
        return domain;
    }

    private UserDeviceTokenJpaEntity toEntity(UserDeviceToken domain) {
        UserDeviceTokenJpaEntity entity = new UserDeviceTokenJpaEntity();
        entity.setId(domain.getId());
        entity.setUserId(domain.getUserId());
        entity.setFcmToken(domain.getFcmToken());
        entity.setDeviceType(domain.getDeviceType());
        entity.setLastActiveAt(domain.getLastActiveAt());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }
}
