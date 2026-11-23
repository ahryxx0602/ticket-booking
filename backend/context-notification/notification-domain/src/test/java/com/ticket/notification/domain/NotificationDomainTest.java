package com.ticket.notification.domain;

import com.ticket.notification.domain.model.DeviceType;
import com.ticket.notification.domain.model.Notification;
import com.ticket.notification.domain.model.NotificationChannel;
import com.ticket.notification.domain.model.NotificationStatus;
import com.ticket.notification.domain.model.NotificationType;
import com.ticket.notification.domain.model.TelegramAccountLink;
import com.ticket.notification.domain.model.UserDeviceToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Notification Domain Model & Invariant Tests")
class NotificationDomainTest {

    @Test
    @DisplayName("Notification should initialize with PENDING status and transition to SENT")
    void testNotificationLifecycle_Sent() {
        Notification notification = Notification.builder()
                .userId(1L)
                .recipient("test@example.com")
                .channel(NotificationChannel.EMAIL)
                .type(NotificationType.PAYMENT_SUCCESS)
                .status(NotificationStatus.PENDING)
                .subject("Payment Success")
                .content("Your booking is confirmed")
                .idempotencyKey("idemp-123")
                .build();

        assertEquals(NotificationStatus.PENDING, notification.getStatus());
        assertEquals(0, notification.getRetryCount());
        assertNull(notification.getSentAt());

        LocalDateTime now = LocalDateTime.now();
        notification.markSent(now);

        assertEquals(NotificationStatus.SENT, notification.getStatus());
        assertEquals(now, notification.getSentAt());
        assertNull(notification.getErrorMessage());
    }

    @Test
    @DisplayName("Notification should mark FAILED and increment retry count")
    void testNotificationLifecycle_FailedAndRetry() {
        Notification notification = Notification.builder()
                .userId(2L)
                .recipient("fcm-token-sample")
                .channel(NotificationChannel.FCM_PUSH)
                .type(NotificationType.PAYMENT_SUCCESS)
                .status(NotificationStatus.PENDING)
                .content("FCM Push message")
                .build();

        LocalDateTime now = LocalDateTime.now();
        notification.markFailed("Connection timeout", now);

        assertEquals(NotificationStatus.FAILED, notification.getStatus());
        assertEquals(1, notification.getRetryCount());
        assertEquals("Connection timeout", notification.getErrorMessage());
        assertTrue(notification.canRetry(3));

        notification.markFailed("Second failure", now);
        assertEquals(2, notification.getRetryCount());
        assertTrue(notification.canRetry(3));

        notification.markFailed("Third failure", now);
        assertEquals(3, notification.getRetryCount());
        assertFalse(notification.canRetry(3));
    }

    @Test
    @DisplayName("Notification should truncate extremely long error messages to 500 characters")
    void testNotification_TruncateErrorMessage() {
        Notification notification = Notification.builder()
                .status(NotificationStatus.PENDING)
                .build();

        String longError = "E".repeat(700);
        notification.markFailed(longError, LocalDateTime.now());

        assertNotNull(notification.getErrorMessage());
        assertEquals(500, notification.getErrorMessage().length());
    }

    @Test
    @DisplayName("UserDeviceToken should update lastActiveAt and reassign ownership")
    void testUserDeviceToken() {
        LocalDateTime t1 = LocalDateTime.of(2026, 11, 22, 10, 0);
        UserDeviceToken token = UserDeviceToken.builder()
                .userId(10L)
                .fcmToken("token-xyz")
                .deviceType(DeviceType.WEB)
                .lastActiveAt(t1)
                .build();

        assertEquals(10L, token.getUserId());
        assertEquals(DeviceType.WEB, token.getDeviceType());

        LocalDateTime t2 = LocalDateTime.of(2026, 11, 22, 12, 0);
        token.refreshActiveAt(t2);
        assertEquals(t2, token.getLastActiveAt());

        LocalDateTime t3 = LocalDateTime.of(2026, 11, 22, 14, 0);
        token.reassignToUser(20L, t3);
        assertEquals(20L, token.getUserId());
        assertEquals(t3, token.getLastActiveAt());
    }

    @Test
    @DisplayName("TelegramAccountLink should activate and deactivate correctly")
    void testTelegramAccountLink() {
        LocalDateTime now = LocalDateTime.now();
        TelegramAccountLink link = TelegramAccountLink.builder()
                .userId(5L)
                .telegramChatId(987654321L)
                .role("ROLE_ADMIN")
                .active(true)
                .linkedAt(now)
                .build();

        assertTrue(link.isActive());
        assertEquals(987654321L, link.getTelegramChatId());

        link.deactivate();
        assertFalse(link.isActive());

        LocalDateTime newTime = now.plusHours(1);
        link.activate(newTime);
        assertTrue(link.isActive());
        assertEquals(newTime, link.getLinkedAt());
    }

    @Test
    @DisplayName("DeviceType fromString should parse safely and fallback to WEB")
    void testDeviceType_FromString() {
        assertEquals(DeviceType.ANDROID, DeviceType.fromString("android"));
        assertEquals(DeviceType.IOS, DeviceType.fromString("IOS"));
        assertEquals(DeviceType.WEB, DeviceType.fromString("web"));
        assertEquals(DeviceType.WEB, DeviceType.fromString(null));
        assertEquals(DeviceType.WEB, DeviceType.fromString("unknown"));
    }
}
