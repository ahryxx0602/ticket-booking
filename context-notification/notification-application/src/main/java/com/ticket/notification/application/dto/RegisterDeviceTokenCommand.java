package com.ticket.notification.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record RegisterDeviceTokenCommand(
        @NotNull(message = "User ID cannot be null")
        Long userId,
        @NotBlank(message = "FCM token cannot be blank")
        String fcmToken,
        String deviceType
) {}
