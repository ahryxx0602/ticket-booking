package com.ticket.notification.application.usecase;

public interface RevokeDeviceTokenUseCase {
    void execute(Long userId, String fcmToken);
}
