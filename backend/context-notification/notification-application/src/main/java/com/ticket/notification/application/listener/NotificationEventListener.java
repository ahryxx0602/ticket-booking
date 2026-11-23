package com.ticket.notification.application.listener;

import com.ticket.notification.application.usecase.SendAdminAlertUseCase;
import com.ticket.notification.application.usecase.SendBookingExpiredNotificationUseCase;
import com.ticket.notification.application.usecase.SendPaymentNotificationUseCase;
import com.ticket.shared.event.BookingExpiredEvent;
import com.ticket.shared.event.PaymentConfirmedEvent;
import com.ticket.shared.event.ReconciliationAlertEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final SendPaymentNotificationUseCase sendPaymentNotificationUseCase;
    private final SendBookingExpiredNotificationUseCase sendBookingExpiredNotificationUseCase;
    private final SendAdminAlertUseCase sendAdminAlertUseCase;

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentConfirmed(PaymentConfirmedEvent event) {
        log.info("[NOTIFICATION EVENT] Received PaymentConfirmedEvent for booking: {}", event.bookingReference());
        try {
            sendPaymentNotificationUseCase.execute(event);
        } catch (Exception ex) {
            log.error("[NOTIFICATION EVENT ERROR] Failed to send payment notification for booking: {}", event.bookingReference(), ex);
        }
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingExpired(BookingExpiredEvent event) {
        log.info("[NOTIFICATION EVENT] Received BookingExpiredEvent for booking: {}", event.bookingReference());
        try {
            sendBookingExpiredNotificationUseCase.execute(event);
        } catch (Exception ex) {
            log.error("[NOTIFICATION EVENT ERROR] Failed to send booking expired notification for booking: {}", event.bookingReference(), ex);
        }
    }

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReconciliationAlert(ReconciliationAlertEvent event) {
        log.info("[NOTIFICATION EVENT] Received ReconciliationAlertEvent for payment: {}", event.paymentReference());
        try {
            sendAdminAlertUseCase.execute(event);
        } catch (Exception ex) {
            log.error("[NOTIFICATION EVENT ERROR] Failed to send reconciliation alert for payment: {}", event.paymentReference(), ex);
        }
    }
}
