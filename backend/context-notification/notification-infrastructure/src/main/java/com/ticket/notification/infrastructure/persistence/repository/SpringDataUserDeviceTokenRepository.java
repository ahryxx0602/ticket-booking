package com.ticket.notification.infrastructure.persistence.repository;

import com.ticket.notification.infrastructure.persistence.entity.UserDeviceTokenJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpringDataUserDeviceTokenRepository extends JpaRepository<UserDeviceTokenJpaEntity, Long> {
    Optional<UserDeviceTokenJpaEntity> findByFcmToken(String fcmToken);
    List<UserDeviceTokenJpaEntity> findByUserId(Long userId);
    void deleteByFcmTokenAndUserId(String fcmToken, Long userId);
    void deleteByFcmTokenIn(List<String> fcmTokens);
}
