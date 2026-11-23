package com.ticket.notification.application.listener;

import com.ticket.notification.application.usecase.SendAdminAlertUseCase;
import com.ticket.notification.application.usecase.SendBookingExpiredNotificationUseCase;
import com.ticket.notification.application.usecase.SendPaymentNotificationUseCase;
import com.ticket.shared.event.BookingExpiredEvent;
import com.ticket.shared.event.PaymentConfirmedEvent;
import com.ticket.shared.event.ReconciliationAlertEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationEventListener Unit Tests")
class NotificationEventListenerTest {

    @Mock
    private SendPaymentNotificationUseCase sendPaymentNotificationUseCase;

    @Mock
    private SendBookingExpiredNotificationUseCase sendBookingExpiredNotificationUseCase;

    @Mock
    private SendAdminAlertUseCase sendAdminAlertUseCase;

    @InjectMocks
    private NotificationEventListener listener;

    @Test
    @DisplayName("onPaymentConfirmed should delegate to usecase and swallow exceptions safely")
    void testOnPaymentConfirmed_SuccessAndErrorHandling() {
        PaymentConfirmedEvent event = new PaymentConfirmedEvent(
                "evt-1", "PAY-01", "BKG-01", 100L, 1L, 10L,
                "customer@example.com", "0912345678", "Customer",
                BigDecimal.valueOf(500000), List.of("A01"), Instant.now()
        );

        // 1. Success case
        assertDoesNotThrow(() -> listener.onPaymentConfirmed(event));
        verify(sendPaymentNotificationUseCase, times(1)).execute(event);

        // 2. Exception case: must not propagate to caller
        doThrow(new RuntimeException("External Gateway Connection Failed"))
                .when(sendPaymentNotificationUseCase).execute(event);
        assertDoesNotThrow(() -> listener.onPaymentConfirmed(event));
    }

    @Test
    @DisplayName("onBookingExpired should delegate to usecase and swallow exceptions safely")
    void testOnBookingExpired_SuccessAndErrorHandling() {
        BookingExpiredEvent event = new BookingExpiredEvent(
                "evt-2", "BKG-EXPIRED-01", 101L, 2L,
                "0987654321", "Customer 2", Instant.now()
        );

        // 1. Success case
        assertDoesNotThrow(() -> listener.onBookingExpired(event));
        verify(sendBookingExpiredNotificationUseCase, times(1)).execute(event);

        // 2. Exception case
        doThrow(new RuntimeException("FCM Service Unavailable"))
                .when(sendBookingExpiredNotificationUseCase).execute(event);
        assertDoesNotThrow(() -> listener.onBookingExpired(event));
    }

    @Test
    @DisplayName("onReconciliationAlert should delegate to usecase and swallow exceptions safely")
    void testOnReconciliationAlert_SuccessAndErrorHandling() {
        ReconciliationAlertEvent event = new ReconciliationAlertEvent(
                "evt-3", "PAY-RECON-01", "BKG-RECON-01", "EXPIRED_PAID",
                BigDecimal.valueOf(1000000), Instant.now()
        );

        // 1. Success case
        assertDoesNotThrow(() -> listener.onReconciliationAlert(event));
        verify(sendAdminAlertUseCase, times(1)).execute(event);

        // 2. Exception case
        doThrow(new RuntimeException("Telegram Bot Timeout"))
                .when(sendAdminAlertUseCase).execute(event);
        assertDoesNotThrow(() -> listener.onReconciliationAlert(event));
    }
}
