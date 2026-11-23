package com.ticket.notification.domain.port;

import java.time.Duration;
import java.util.Optional;

public interface TelegramLinkTokenPort {
    record LinkTokenPayload(Long userId, String role, String username) {}

    String generateToken(Long userId, String role, String username, Duration ttl);
    Optional<LinkTokenPayload> verifyAndConsumeToken(String token);
}
