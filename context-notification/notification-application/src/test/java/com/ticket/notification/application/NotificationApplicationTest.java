package com.ticket.notification.application;

import com.ticket.notification.application.dto.GenerateLinkTokenResponse;
import com.ticket.notification.application.dto.RegisterDeviceTokenCommand;
import com.ticket.notification.application.dto.TelegramInboundUpdateDto;
import com.ticket.notification.application.service.NotificationCommandServiceImpl;
import com.ticket.notification.domain.model.DeviceType;
import com.ticket.notification.domain.model.Notification;
import com.ticket.notification.domain.model.TelegramAccountLink;
import com.ticket.notification.domain.model.UserDeviceToken;
import com.ticket.notification.domain.port.*;
import com.ticket.shared.dto.PageResponse;
import com.ticket.shared.event.PaymentConfirmedEvent;
import com.ticket.shared.event.ReconciliationAlertEvent;
import com.ticket.shared.exception.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Notification Application Service Unit Tests")
class NotificationApplicationTest {

    @Mock private UserDeviceTokenRepository userDeviceTokenRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private TelegramAccountLinkRepository telegramAccountLinkRepository;
    @Mock private TelegramLinkTokenPort telegramLinkTokenPort;
    @Mock private PushNotificationPort pushNotificationPort;
    @Mock private EmailSenderPort emailSenderPort;
    @Mock private TelegramBotPort telegramBotPort;
    @Mock private NotificationCatalogPort notificationCatalogPort;
    @Mock private NotificationBookingPort notificationBookingPort;
    @Mock private NotificationIdentityPort notificationIdentityPort;

    private NotificationCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new NotificationCommandServiceImpl(
                userDeviceTokenRepository,
                notificationRepository,
                telegramAccountLinkRepository,
                telegramLinkTokenPort,
                pushNotificationPort,
                emailSenderPort,
                telegramBotPort,
                notificationCatalogPort,
                notificationBookingPort,
                notificationIdentityPort
        );
    }

    @Test
    @DisplayName("Register new FCM Device Token should save successfully")
    void testRegisterDeviceToken_NewToken() {
        when(userDeviceTokenRepository.findByFcmToken("fcm-token-123")).thenReturn(Optional.empty());

        RegisterDeviceTokenCommand command = RegisterDeviceTokenCommand.builder()
                .userId(1L)
                .fcmToken("fcm-token-123")
                .deviceType("ANDROID")
                .build();

        service.execute(command);

        verify(userDeviceTokenRepository).save(argThat(token ->
                token.getUserId().equals(1L) &&
                token.getFcmToken().equals("fcm-token-123") &&
                token.getDeviceType() == DeviceType.ANDROID
        ));
    }

    @Test
    @DisplayName("Register existing FCM Device Token with different user should reassign ownership")
    void testRegisterDeviceToken_ReassignOwnership() {
        UserDeviceToken existing = UserDeviceToken.builder()
                .id(10L)
                .userId(1L)
                .fcmToken("fcm-token-123")
                .deviceType(DeviceType.WEB)
                .lastActiveAt(LocalDateTime.now().minusDays(1))
                .build();

        when(userDeviceTokenRepository.findByFcmToken("fcm-token-123")).thenReturn(Optional.of(existing));

        RegisterDeviceTokenCommand command = RegisterDeviceTokenCommand.builder()
                .userId(2L)
                .fcmToken("fcm-token-123")
                .deviceType("WEB")
                .build();

        service.execute(command);

        assertEquals(2L, existing.getUserId());
        verify(userDeviceTokenRepository).save(existing);
    }

    @Test
    @DisplayName("Generate Telegram Link Token for Admin should succeed")
    void testGenerateTelegramLinkToken_AdminSuccess() {
        NotificationIdentityPort.UserProfileSnapshot user = new NotificationIdentityPort.UserProfileSnapshot(
                1L, "admin", "admin@ticket.com", "Admin User", "0912345678", "ROLE_ADMIN", false
        );
        when(notificationIdentityPort.getUserProfile(1L)).thenReturn(Optional.of(user));
        when(telegramLinkTokenPort.generateToken(eq(1L), eq("ROLE_ADMIN"), eq("admin"), any(Duration.class)))
                .thenReturn("TOKEN123");

        GenerateLinkTokenResponse response = service.execute(1L);

        assertNotNull(response);
        assertEquals("TOKEN123", response.token());
        assertTrue(response.instructions().contains("/link TOKEN123"));
    }

    @Test
    @DisplayName("Generate Telegram Link Token for Locked User should throw ForbiddenException")
    void testGenerateTelegramLinkToken_LockedUser() {
        NotificationIdentityPort.UserProfileSnapshot user = new NotificationIdentityPort.UserProfileSnapshot(
                2L, "locked_admin", "locked@ticket.com", "Locked Admin", "0912345678", "ROLE_ADMIN", true
        );
        when(notificationIdentityPort.getUserProfile(2L)).thenReturn(Optional.of(user));

        assertThrows(ForbiddenException.class, () -> service.execute(2L));
        verify(telegramLinkTokenPort, never()).generateToken(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Process Telegram /link with valid token should link chat_id successfully")
    void testProcessTelegramUpdate_LinkSuccess() {
        TelegramLinkTokenPort.LinkTokenPayload payload = new TelegramLinkTokenPort.LinkTokenPayload(
                1L, "ROLE_ADMIN", "admin"
        );
        when(telegramLinkTokenPort.verifyAndConsumeToken("TOKEN123")).thenReturn(Optional.of(payload));

        NotificationIdentityPort.UserProfileSnapshot user = new NotificationIdentityPort.UserProfileSnapshot(
                1L, "admin", "admin@ticket.com", "Admin User", "0912345678", "ROLE_ADMIN", false
        );
        when(notificationIdentityPort.getUserProfile(1L)).thenReturn(Optional.of(user));
        when(telegramAccountLinkRepository.findByTelegramChatId(999888L)).thenReturn(Optional.empty());

        TelegramInboundUpdateDto update = new TelegramInboundUpdateDto(
                1001L, 999888L, "/link TOKEN123", "tg_admin", 999888L
        );

        service.execute(update);

        verify(telegramAccountLinkRepository).save(argThat(link ->
                link.getUserId().equals(1L) &&
                link.getTelegramChatId().equals(999888L) &&
                "ROLE_ADMIN".equals(link.getRole()) &&
                link.isActive()
        ));
        verify(telegramBotPort).sendMessage(eq(999888L), contains("Liên kết thành công"));
    }

    @Test
    @DisplayName("Process Telegram /ve with matching phone should return booking details")
    void testProcessTelegramUpdate_LookupSuccess() {
        NotificationBookingPort.BookingDetailsSnapshot booking = new NotificationBookingPort.BookingDetailsSnapshot(
                10L, "BKG_123456", 1L, 5L, "CONFIRMED", BigDecimal.valueOf(500000), "0912345678", LocalDateTime.now(), List.of("A01", "A02")
        );
        when(notificationBookingPort.getBookingDetailsByReferenceAndPhone("BKG_123456", "0912345678"))
                .thenReturn(Optional.of(booking));

        TelegramInboundUpdateDto update = new TelegramInboundUpdateDto(
                1002L, 999888L, "/ve BKG_123456 0912345678", "tg_user", 999888L
        );

        service.execute(update);

        verify(telegramBotPort).sendMessage(eq(999888L), contains("BKG_123456"));
    }

    @Test
    @DisplayName("PaymentConfirmedEvent should trigger Email, FCM push and Telegram alert")
    void testPaymentConfirmedNotificationFlow() {
        PaymentConfirmedEvent event = new PaymentConfirmedEvent(
                "evt-1",
                "PAY_001",
                "BKG_999",
                10L,
                1L,
                5L,
                "customer@example.com",
                "0912345678",
                "Nguyen Van A",
                BigDecimal.valueOf(350000),
                List.of("B01"),
                Instant.now()
        );

        when(notificationRepository.findByIdempotencyKey("email:pay:BKG_999")).thenReturn(Optional.empty());
        when(userDeviceTokenRepository.findByUserId(1L)).thenReturn(List.of(
                UserDeviceToken.builder().userId(1L).fcmToken("fcm-tok-1").deviceType(DeviceType.ANDROID).build()
        ));
        when(notificationRepository.findByIdempotencyKey("fcm:pay:BKG_999")).thenReturn(Optional.empty());

        service.execute(event);

        verify(emailSenderPort).sendETicketEmail(eq("customer@example.com"), contains("BKG_999"), anyMap(), eq("BKG_999"));
        verify(pushNotificationPort).sendMulticast(eq(List.of("fcm-tok-1")), contains("thành công"), anyString(), anyMap());
        verify(telegramBotPort).sendAlertToAdmins(contains("BKG_999"));
    }

    @Test
    @DisplayName("ReconciliationAlertEvent should send alert to Telegram Admins")
    void testReconciliationAlertFlow() {
        ReconciliationAlertEvent event = new ReconciliationAlertEvent(
                "evt-2",
                "PAY_REC_01",
                "BKG_EXP_01",
                "PAID_AFTER_BOOKING_EXPIRED",
                BigDecimal.valueOf(450000),
                Instant.now()
        );

        service.execute(event);

        verify(telegramBotPort).sendAlertToAdmins(contains("CẢNH BÁO ĐỐI SOÁT"));
    }
}
