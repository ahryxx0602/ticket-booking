package com.ticket.notification.infrastructure.persistence;

import com.ticket.notification.domain.model.DeviceType;
import com.ticket.notification.domain.model.Notification;
import com.ticket.notification.domain.model.NotificationChannel;
import com.ticket.notification.domain.model.NotificationStatus;
import com.ticket.notification.domain.model.NotificationType;
import com.ticket.notification.domain.model.TelegramAccountLink;
import com.ticket.notification.domain.model.UserDeviceToken;
import com.ticket.notification.infrastructure.persistence.adapter.NotificationRepositoryImpl;
import com.ticket.notification.infrastructure.persistence.adapter.TelegramAccountLinkRepositoryImpl;
import com.ticket.notification.infrastructure.persistence.adapter.UserDeviceTokenRepositoryImpl;
import com.ticket.notification.infrastructure.persistence.entity.NotificationJpaEntity;
import com.ticket.notification.infrastructure.persistence.entity.TelegramAccountLinkJpaEntity;
import com.ticket.notification.infrastructure.persistence.entity.UserDeviceTokenJpaEntity;
import com.ticket.notification.infrastructure.persistence.repository.SpringDataNotificationRepository;
import com.ticket.notification.infrastructure.persistence.repository.SpringDataTelegramAccountLinkRepository;
import com.ticket.notification.infrastructure.persistence.repository.SpringDataUserDeviceTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Notification Persistence Adapter Unit Tests")
class NotificationPersistenceTest {

    @Mock private SpringDataNotificationRepository notificationJpaRepo;
    @Mock private SpringDataUserDeviceTokenRepository userDeviceTokenJpaRepo;
    @Mock private SpringDataTelegramAccountLinkRepository telegramLinkJpaRepo;

    @InjectMocks private NotificationRepositoryImpl notificationRepo;
    @InjectMocks private UserDeviceTokenRepositoryImpl userDeviceTokenRepo;
    @InjectMocks private TelegramAccountLinkRepositoryImpl telegramLinkRepo;

    @Test
    @DisplayName("NotificationRepository save and findByIdempotencyKey should map correctly")
    void testNotificationRepository() {
        NotificationJpaEntity entity = new NotificationJpaEntity();
        entity.setId(1L);
        entity.setUserId(10L);
        entity.setRecipient("user@example.com");
        entity.setChannel(NotificationChannel.EMAIL);
        entity.setType(NotificationType.PAYMENT_SUCCESS);
        entity.setStatus(NotificationStatus.SENT);
        entity.setIdempotencyKey("idemp-1");
        entity.setCreatedAt(LocalDateTime.now());

        when(notificationJpaRepo.save(any(NotificationJpaEntity.class))).thenReturn(entity);
        when(notificationJpaRepo.findByIdempotencyKey("idemp-1")).thenReturn(Optional.of(entity));

        Notification saved = notificationRepo.save(Notification.builder()
                .userId(10L)
                .recipient("user@example.com")
                .channel(NotificationChannel.EMAIL)
                .type(NotificationType.PAYMENT_SUCCESS)
                .status(NotificationStatus.SENT)
                .idempotencyKey("idemp-1")
                .build());

        assertNotNull(saved);
        assertEquals(1L, saved.getId());

        Optional<Notification> found = notificationRepo.findByIdempotencyKey("idemp-1");
        assertTrue(found.isPresent());
        assertEquals("user@example.com", found.get().getRecipient());
    }

    @Test
    @DisplayName("UserDeviceTokenRepository save and findByFcmToken should map correctly")
    void testUserDeviceTokenRepository() {
        UserDeviceTokenJpaEntity entity = new UserDeviceTokenJpaEntity();
        entity.setId(2L);
        entity.setUserId(10L);
        entity.setFcmToken("fcm-abc-123");
        entity.setDeviceType(DeviceType.WEB);
        entity.setLastActiveAt(LocalDateTime.now());

        when(userDeviceTokenJpaRepo.save(any(UserDeviceTokenJpaEntity.class))).thenReturn(entity);
        when(userDeviceTokenJpaRepo.findByFcmToken("fcm-abc-123")).thenReturn(Optional.of(entity));

        UserDeviceToken token = userDeviceTokenRepo.save(UserDeviceToken.builder()
                .userId(10L)
                .fcmToken("fcm-abc-123")
                .deviceType(DeviceType.WEB)
                .build());

        assertNotNull(token);
        assertEquals(2L, token.getId());

        Optional<UserDeviceToken> found = userDeviceTokenRepo.findByFcmToken("fcm-abc-123");
        assertTrue(found.isPresent());
        assertEquals(DeviceType.WEB, found.get().getDeviceType());
    }

    @Test
    @DisplayName("TelegramAccountLinkRepository save and findByTelegramChatId should map correctly")
    void testTelegramAccountLinkRepository() {
        TelegramAccountLinkJpaEntity entity = new TelegramAccountLinkJpaEntity();
        entity.setId(3L);
        entity.setUserId(1L);
        entity.setTelegramChatId(123456789L);
        entity.setRole("ROLE_ADMIN");
        entity.setActive(true);
        entity.setLinkedAt(LocalDateTime.now());

        when(telegramLinkJpaRepo.save(any(TelegramAccountLinkJpaEntity.class))).thenReturn(entity);
        when(telegramLinkJpaRepo.findByTelegramChatId(123456789L)).thenReturn(Optional.of(entity));

        TelegramAccountLink link = telegramLinkRepo.save(TelegramAccountLink.builder()
                .userId(1L)
                .telegramChatId(123456789L)
                .role("ROLE_ADMIN")
                .active(true)
                .build());

        assertNotNull(link);
        assertEquals(123456789L, link.getTelegramChatId());

        Optional<TelegramAccountLink> found = telegramLinkRepo.findByTelegramChatId(123456789L);
        assertTrue(found.isPresent());
        assertEquals("ROLE_ADMIN", found.get().getRole());
    }
}
