package com.ticket.notification.application.dto;

import java.time.LocalDateTime;

public record GenerateLinkTokenResponse(
        String token,
        LocalDateTime expiresAt,
        Long expiresInSeconds,
        String botUsername,
        String deepLink,
        String instructions
) {
    public GenerateLinkTokenResponse(String token, LocalDateTime expiresAt, String botUsername, String instructions) {
        this(token, expiresAt, 600L, botUsername, "https://t.me/" + botUsername + "?start=" + token, instructions);
    }
}
