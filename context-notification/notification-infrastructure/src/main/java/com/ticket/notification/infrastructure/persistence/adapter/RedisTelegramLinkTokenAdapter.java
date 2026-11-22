package com.ticket.notification.infrastructure.persistence.adapter;

import com.ticket.notification.domain.port.TelegramLinkTokenPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisTelegramLinkTokenAdapter implements TelegramLinkTokenPort {

    private static final String KEY_PREFIX = "noti:tele-link:";
    private static final String CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // Loại bỏ ký tự dễ nhầm lẫn như 0, O, 1, I
    private static final int TOKEN_LENGTH = 8;

    private final StringRedisTemplate redisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generateToken(Long userId, String role, String username, Duration ttl) {
        String token = generateRandomToken();
        String key = KEY_PREFIX + token;
        // Format lưu trữ: userId:role:username
        String value = userId + ":" + role + ":" + (username != null ? username : "");
        redisTemplate.opsForValue().set(key, value, ttl);
        log.info("[TELEGRAM LINK] Generated link token for userId {} (role {}), ttl: {}s", userId, role, ttl.toSeconds());
        return token;
    }

    @Override
    public Optional<LinkTokenPayload> verifyAndConsumeToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return Optional.empty();
        }
        String key = KEY_PREFIX + token.trim().toUpperCase();
        // Atomic GETDEL
        String value = redisTemplate.opsForValue().getAndDelete(key);
        if (value == null) {
            return Optional.empty();
        }

        try {
            String[] parts = value.split(":");
            Long userId = Long.parseLong(parts[0]);
            String role = parts[1];
            String username = parts.length > 2 ? parts[2] : "";
            return Optional.of(new LinkTokenPayload(userId, role, username));
        } catch (Exception ex) {
            log.error("[TELEGRAM LINK ERROR] Failed to parse token value: {}", value, ex);
            return Optional.empty();
        }
    }

    private String generateRandomToken() {
        StringBuilder sb = new StringBuilder(TOKEN_LENGTH);
        for (int i = 0; i < TOKEN_LENGTH; i++) {
            sb.append(CHARACTERS.charAt(secureRandom.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }
}
