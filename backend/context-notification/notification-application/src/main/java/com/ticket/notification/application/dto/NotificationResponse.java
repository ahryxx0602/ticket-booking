package com.ticket.notification.application.dto;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        Long userId,
        String recipient,
        String channel,
        String type,
        String status,
        String subject,
        String content,
        LocalDateTime sentAt,
        LocalDateTime createdAt
) {}
