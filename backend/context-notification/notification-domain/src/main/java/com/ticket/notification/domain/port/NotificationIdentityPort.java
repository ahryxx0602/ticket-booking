package com.ticket.notification.domain.port;

import java.util.Optional;

public interface NotificationIdentityPort {
    record UserProfileSnapshot(
            Long userId,
            String username,
            String email,
            String fullName,
            String phone,
            String role,
            boolean locked
    ) {}

    Optional<UserProfileSnapshot> getUserProfile(Long userId);
    Optional<UserProfileSnapshot> getUserProfileByUsername(String username);
}
