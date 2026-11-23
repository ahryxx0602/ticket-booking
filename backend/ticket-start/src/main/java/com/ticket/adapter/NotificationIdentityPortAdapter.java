package com.ticket.adapter;

import com.ticket.identity.domain.model.User;
import com.ticket.identity.domain.repository.UserRepository;
import com.ticket.notification.domain.port.NotificationIdentityPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class NotificationIdentityPortAdapter implements NotificationIdentityPort {

    private final UserRepository userRepository;

    @Override
    public Optional<UserProfileSnapshot> getUserProfile(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return userRepository.findById(userId).map(this::toSnapshot);
    }

    @Override
    public Optional<UserProfileSnapshot> getUserProfileByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByUsername(username).map(this::toSnapshot);
    }

    private UserProfileSnapshot toSnapshot(User u) {
        return new UserProfileSnapshot(
                u.getId(),
                u.getUsername(),
                u.getEmail(),
                u.getFullName(),
                u.getPhone(),
                u.getRole() != null ? u.getRole().name() : null,
                u.isLocked()
        );
    }
}
