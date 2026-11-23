package com.ticket.notification.domain.port;

import com.ticket.notification.domain.model.UserDeviceToken;

import java.util.List;
import java.util.Optional;

public interface UserDeviceTokenRepository {
    UserDeviceToken save(UserDeviceToken token);
    Optional<UserDeviceToken> findByFcmToken(String fcmToken);
    List<UserDeviceToken> findByUserId(Long userId);
    void deleteByFcmTokenAndUserId(String fcmToken, Long userId);
    void deleteByFcmTokens(List<String> fcmTokens);
}
