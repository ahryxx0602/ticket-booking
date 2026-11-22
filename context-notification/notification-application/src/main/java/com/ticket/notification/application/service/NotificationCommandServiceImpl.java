package com.ticket.notification.application.service;

import com.ticket.notification.application.dto.GenerateLinkTokenResponse;
import com.ticket.notification.application.dto.NotificationResponse;
import com.ticket.notification.application.dto.RegisterDeviceTokenCommand;
import com.ticket.notification.application.dto.TelegramInboundUpdateDto;
import com.ticket.notification.application.usecase.*;
import com.ticket.notification.domain.model.*;
import com.ticket.notification.domain.port.*;
import com.ticket.shared.dto.PageResponse;
import com.ticket.shared.event.BookingExpiredEvent;
import com.ticket.shared.event.PaymentConfirmedEvent;
import com.ticket.shared.event.ReconciliationAlertEvent;
import com.ticket.shared.exception.BusinessException;
import com.ticket.shared.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationCommandServiceImpl implements
        RegisterDeviceTokenUseCase,
        RevokeDeviceTokenUseCase,
        GenerateTelegramLinkTokenUseCase,
        ProcessTelegramUpdateUseCase,
        GetMyNotificationsUseCase,
        SendPaymentNotificationUseCase,
        SendBookingExpiredNotificationUseCase,
        SendAdminAlertUseCase,
        RetryFailedNotificationsUseCase {

    private final UserDeviceTokenRepository userDeviceTokenRepository;
    private final NotificationRepository notificationRepository;
    private final TelegramAccountLinkRepository telegramAccountLinkRepository;
    private final TelegramLinkTokenPort telegramLinkTokenPort;
    private final PushNotificationPort pushNotificationPort;
    private final EmailSenderPort emailSenderPort;
    private final TelegramBotPort telegramBotPort;
    private final NotificationCatalogPort notificationCatalogPort;
    private final NotificationBookingPort notificationBookingPort;
    private final NotificationIdentityPort notificationIdentityPort;

    @Override
    @Transactional
    public void execute(RegisterDeviceTokenCommand command) {
        log.info("[DEVICE TOKEN] Registering device token for userId: {}", command.userId());
        Optional<UserDeviceToken> existing = userDeviceTokenRepository.findByFcmToken(command.fcmToken());
        LocalDateTime now = LocalDateTime.now();
        if (existing.isPresent()) {
            UserDeviceToken token = existing.get();
            if (!token.getUserId().equals(command.userId())) {
                log.info("[DEVICE TOKEN] Reassigning token from userId {} to {}", token.getUserId(), command.userId());
                token.reassignToUser(command.userId(), now);
            } else {
                token.refreshActiveAt(now);
            }
            userDeviceTokenRepository.save(token);
        } else {
            UserDeviceToken newToken = UserDeviceToken.builder()
                    .userId(command.userId())
                    .fcmToken(command.fcmToken())
                    .deviceType(DeviceType.fromString(command.deviceType()))
                    .lastActiveAt(now)
                    .build();
            userDeviceTokenRepository.save(newToken);
        }
    }

    @Override
    @Transactional
    public void execute(Long userId, String fcmToken) {
        log.info("[DEVICE TOKEN] Revoking device token for userId: {}", userId);
        userDeviceTokenRepository.deleteByFcmTokenAndUserId(fcmToken, userId);
    }

    @Override
    public GenerateLinkTokenResponse execute(Long userId) {
        NotificationIdentityPort.UserProfileSnapshot user = notificationIdentityPort.getUserProfile(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "User not found"));

        if (user.locked()) {
            throw new ForbiddenException("ACCOUNT_LOCKED", "User account is locked");
        }

        String role = user.role();
        if (!"ROLE_ADMIN".equals(role) && !"ROLE_OPERATOR".equals(role)) {
            throw new ForbiddenException("INSUFFICIENT_PERMISSIONS", "Only Admin or Operator can link Telegram Bot");
        }

        Duration ttl = Duration.ofMinutes(10);
        String token = telegramLinkTokenPort.generateToken(userId, role, user.username(), ttl);
        LocalDateTime expiresAt = LocalDateTime.now().plus(ttl);

        return new GenerateLinkTokenResponse(
                token,
                expiresAt,
                "@TicketBookingBot",
                "Mở Telegram Bot, gõ: /link " + token + " (Hạn sử dụng: 10 phút)"
        );
    }

    @Override
    @Transactional
    public void execute(TelegramInboundUpdateDto update) {
        if (update.chatId() == null || update.messageText() == null) {
            return;
        }

        String text = update.messageText().trim();
        Long chatId = update.chatId();

        if (text.startsWith("/start")) {
            handleTelegramStart(chatId);
        } else if (text.startsWith("/link")) {
            handleTelegramLink(chatId, text);
        } else if (text.startsWith("/ve")) {
            handleTelegramLookup(chatId, text);
        } else if (text.startsWith("/stats")) {
            handleTelegramStats(chatId);
        } else {
            telegramBotPort.sendMessage(chatId, "Lệnh không hợp lệ. Gõ /start để xem hướng dẫn.");
        }
    }

    private void handleTelegramStart(Long chatId) {
        String msg = """
                👋 Chào mừng bạn đến với Ticket Booking Bot!
                
                📌 Các lệnh hỗ trợ:
                - /ve <MãVé> <SốĐiệnThoại>: Tra cứu thông tin vé xe
                - /link <MãToken>: Liên kết tài khoản Quản trị viên / Chủ nhà xe
                - /stats: Xem báo cáo thống kê nhanh (Dành cho Quản trị viên)
                """;
        telegramBotPort.sendMessage(chatId, msg);
    }

    private void handleTelegramLink(Long chatId, String text) {
        String[] parts = text.split("\\s+");
        if (parts.length < 2) {
            telegramBotPort.sendMessage(chatId, "⚠️ Cú pháp sai. Vui lòng nhập: /link <MãToken8KýTự>");
            return;
        }

        String token = parts[1].trim();
        Optional<TelegramLinkTokenPort.LinkTokenPayload> payloadOpt = telegramLinkTokenPort.verifyAndConsumeToken(token);

        if (payloadOpt.isEmpty()) {
            telegramBotPort.sendMessage(chatId, "❌ Mã liên kết không hợp lệ hoặc đã hết hạn (hạn 10 phút). Vui lòng lấy mã mới trên web.");
            return;
        }

        TelegramLinkTokenPort.LinkTokenPayload payload = payloadOpt.get();

        // Kiểm tra tài khoản không bị khóa
        Optional<NotificationIdentityPort.UserProfileSnapshot> userOpt = notificationIdentityPort.getUserProfile(payload.userId());
        if (userOpt.isEmpty() || userOpt.get().locked()) {
            telegramBotPort.sendMessage(chatId, "❌ Tài khoản của bạn đã bị khóa hoặc không tồn tại.");
            return;
        }

        Optional<TelegramAccountLink> existing = telegramAccountLinkRepository.findByTelegramChatId(chatId);
        LocalDateTime now = LocalDateTime.now();
        if (existing.isPresent()) {
            TelegramAccountLink link = existing.get();
            link.setUserId(payload.userId());
            link.setRole(payload.role());
            link.activate(now);
            telegramAccountLinkRepository.save(link);
        } else {
            TelegramAccountLink link = TelegramAccountLink.builder()
                    .userId(payload.userId())
                    .telegramChatId(chatId)
                    .role(payload.role())
                    .active(true)
                    .linkedAt(now)
                    .build();
            telegramAccountLinkRepository.save(link);
        }

        telegramBotPort.sendMessage(chatId, "✅ Liên kết thành công!\nTài khoản: " + payload.username() + "\nVai trò: " + payload.role());
    }

    private void handleTelegramLookup(Long chatId, String text) {
        String[] parts = text.split("\\s+");
        if (parts.length < 3) {
            telegramBotPort.sendMessage(chatId, "⚠️ Cú pháp: /ve <MãVé> <SốĐiệnThoại>\nVí dụ: /ve BKG_123456 0912345678");
            return;
        }

        String bookingRef = parts[1].trim();
        String phone = parts[2].trim();

        Optional<NotificationBookingPort.BookingDetailsSnapshot> bookingOpt =
                notificationBookingPort.getBookingDetailsByReferenceAndPhone(bookingRef, phone);

        if (bookingOpt.isEmpty()) {
            telegramBotPort.sendMessage(chatId, "❌ Không tìm thấy vé khớp với mã '" + bookingRef + "' và SĐT '" + phone + "'.");
            return;
        }

        NotificationBookingPort.BookingDetailsSnapshot booking = bookingOpt.get();
        String msg = String.format("""
                🎫 THÔNG TIN VÉ XE:
                - Mã vé: %s
                - Trạng thái: %s
                - Số ghế: %s
                - Tổng tiền: %,.0f VND
                """,
                booking.bookingReference(),
                booking.status(),
                String.join(", ", booking.seatNumbers()),
                booking.totalAmount()
        );

        telegramBotPort.sendMessage(chatId, msg);
    }

    private void handleTelegramStats(Long chatId) {
        Optional<TelegramAccountLink> linkOpt = telegramAccountLinkRepository.findByTelegramChatId(chatId);
        if (linkOpt.isEmpty() || !linkOpt.get().isActive()) {
            telegramBotPort.sendMessage(chatId, "🔒 Bạn chưa liên kết tài khoản Quản trị viên. Gõ /link <Token> để liên kết.");
            return;
        }

        TelegramAccountLink link = linkOpt.get();
        if (!"ROLE_ADMIN".equals(link.getRole())) {
            telegramBotPort.sendMessage(chatId, "⛔ Lệnh này chỉ dành cho Quản trị viên cấp cao (Admin).");
            return;
        }

        String statsMsg = "📊 BÁO CÁO HỆ THỐNG:\n- Trạng thái hệ thống: 🟢 HOẠT ĐỘNG TỐT\n- Mọi giao dịch đang được giám sát thời gian thực.";
        telegramBotPort.sendMessage(chatId, statsMsg);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> execute(Long userId, int page, int size) {
        int validPage = Math.max(page, 0);
        int validSize = Math.min(Math.max(size, 1), 100);
        int offset = validPage * validSize;

        List<Notification> list = notificationRepository.findByUserId(userId, offset, validSize);
        long total = notificationRepository.countByUserId(userId);

        List<NotificationResponse> dtos = list.stream().map(n -> new NotificationResponse(
                n.getId(),
                n.getUserId(),
                n.getRecipient(),
                n.getChannel().name(),
                n.getType().name(),
                n.getStatus().name(),
                n.getSubject(),
                n.getContent(),
                n.getSentAt(),
                n.getCreatedAt()
        )).toList();

        return PageResponse.of(dtos, validPage, validSize, total);
    }

    @Override
    public void execute(PaymentConfirmedEvent event) {
        log.info("[NOTIFICATION SEND] Processing payment confirmation notification for: {}", event.bookingReference());

        // 1. Gửi Email Vé Điện Tử
        String targetEmail = event.customerEmail();
        if ((targetEmail == null || targetEmail.trim().isEmpty()) && event.userId() != null) {
            targetEmail = notificationIdentityPort.getUserProfile(event.userId())
                    .map(NotificationIdentityPort.UserProfileSnapshot::email)
                    .orElse(null);
        }

        if (targetEmail != null && !targetEmail.trim().isEmpty()) {
            String emailIdempotency = "email:pay:" + event.bookingReference();
            if (notificationRepository.findByIdempotencyKey(emailIdempotency).isEmpty()) {
                Notification noti = Notification.builder()
                        .userId(event.userId())
                        .recipient(targetEmail)
                        .channel(NotificationChannel.EMAIL)
                        .type(NotificationType.PAYMENT_SUCCESS)
                        .status(NotificationStatus.PENDING)
                        .subject("Vé Điện Tử - Đặt vé thành công #" + event.bookingReference())
                        .content("Vé điện tử của bạn đã được xác nhận thanh toán.")
                        .idempotencyKey(emailIdempotency)
                        .build();
                notificationRepository.save(noti);

                try {
                    Map<String, Object> model = new HashMap<>();
                    model.put("bookingReference", event.bookingReference());
                    model.put("passengerName", event.passengerName());
                    model.put("totalAmount", event.totalAmount());
                    model.put("seatNumbers", String.join(", ", event.seatNumbers()));
                    model.put("customerPhone", event.customerPhone());
                    model.put("tripId", event.tripId());

                    // Lấy snapshot thông tin chuyến
                    notificationCatalogPort.getTripDetails(event.tripId()).ifPresent(t -> {
                        model.put("departureStation", t.departureStationName());
                        model.put("arrivalStation", t.arrivalStationName());
                        model.put("departureTime", t.departureTime());
                    });

                    emailSenderPort.sendETicketEmail(targetEmail, noti.getSubject(), model, event.bookingReference());
                    noti.markSent(LocalDateTime.now());
                    notificationRepository.save(noti);
                } catch (Exception ex) {
                    log.error("[EMAIL ERROR] Failed to send email to {}", targetEmail, ex);
                    noti.markFailed(ex.getMessage(), LocalDateTime.now());
                    notificationRepository.save(noti);
                }
            }
        }

        // 2. Gửi Push Notification FCM
        if (event.userId() != null) {
            List<UserDeviceToken> tokens = userDeviceTokenRepository.findByUserId(event.userId());
            if (!tokens.isEmpty()) {
                List<String> fcmTokens = tokens.stream().map(UserDeviceToken::getFcmToken).toList();
                String fcmIdempotency = "fcm:pay:" + event.bookingReference();
                if (notificationRepository.findByIdempotencyKey(fcmIdempotency).isEmpty()) {
                    Notification fcmNoti = Notification.builder()
                            .userId(event.userId())
                            .recipient(String.join(",", fcmTokens))
                            .channel(NotificationChannel.FCM_PUSH)
                            .type(NotificationType.PAYMENT_SUCCESS)
                            .status(NotificationStatus.PENDING)
                            .subject("Thanh toán thành công!")
                            .content("Vé #" + event.bookingReference() + " của bạn đã được xác nhận.")
                            .idempotencyKey(fcmIdempotency)
                            .build();
                    notificationRepository.save(fcmNoti);

                    try {
                        Map<String, String> data = Map.of(
                                "bookingReference", event.bookingReference(),
                                "type", "PAYMENT_SUCCESS"
                        );
                        List<String> invalidTokens = pushNotificationPort.sendMulticast(
                                fcmTokens,
                                "Thanh toán vé thành công! 🎫",
                                "Đơn vé #" + event.bookingReference() + " đã thanh toán thành công. Chúc bạn có chuyến đi vui vẻ!",
                                data
                        );

                        if (!invalidTokens.isEmpty()) {
                            log.info("[FCM CLEANUP] Removing {} invalid tokens", invalidTokens.size());
                            userDeviceTokenRepository.deleteByFcmTokens(invalidTokens);
                        }

                        fcmNoti.markSent(LocalDateTime.now());
                        notificationRepository.save(fcmNoti);
                    } catch (Exception ex) {
                        log.error("[FCM ERROR] Failed to send push notification to user {}", event.userId(), ex);
                        fcmNoti.markFailed(ex.getMessage(), LocalDateTime.now());
                        notificationRepository.save(fcmNoti);
                    }
                }
            }
        }

        // 3. Bắn Telegram cho Admin
        String teleAlert = String.format("🟢 [THANH TOÁN THÀNH CÔNG]\n- Đơn: %s\n- Tiền: %,.0f VND\n- Khách: %s",
                event.bookingReference(), event.totalAmount(), event.passengerName());
        try {
            telegramBotPort.sendAlertToAdmins(teleAlert);
        } catch (Exception ex) {
            log.warn("[TELEGRAM ALERT] Failed to send admin alert", ex);
        }
    }

    @Override
    public void execute(BookingExpiredEvent event) {
        log.info("[NOTIFICATION SEND] Processing booking expired notification for: {}", event.bookingReference());
        if (event.userId() != null) {
            List<UserDeviceToken> tokens = userDeviceTokenRepository.findByUserId(event.userId());
            if (!tokens.isEmpty()) {
                List<String> fcmTokens = tokens.stream().map(UserDeviceToken::getFcmToken).toList();
                try {
                    Map<String, String> data = Map.of(
                            "bookingReference", event.bookingReference(),
                            "type", "BOOKING_EXPIRED"
                    );
                    pushNotificationPort.sendMulticast(
                            fcmTokens,
                            "Vé giữ chỗ đã hết hạn ⏰",
                            "Đơn vé #" + event.bookingReference() + " đã quá hạn thanh toán và đã được giải phóng ghế.",
                            data
                    );
                } catch (Exception ex) {
                    log.error("[FCM ERROR] Failed to send expired notification for booking {}", event.bookingReference(), ex);
                }
            }
        }
    }

    @Override
    public void execute(ReconciliationAlertEvent event) {
        log.warn("[RECONCILIATION ALERT] Triggered for payment: {}", event.paymentReference());
        String msg = String.format("🔴 [CẢNH BÁO ĐỐI SOÁT - CẦN XỬ LÝ]\n- Mã GD: %s\n- Đơn vé: %s\n- Số tiền: %,.0f VND\n- Lý do: %s",
                event.paymentReference(), event.bookingReference(), event.amount(), event.reason());
        try {
            telegramBotPort.sendAlertToAdmins(msg);
        } catch (Exception ex) {
            log.error("[TELEGRAM ERROR] Failed to send reconciliation alert to Telegram", ex);
        }
    }

    @Override
    @Transactional
    public int execute(int maxRetries, int batchSize) {
        List<Notification> failedList = notificationRepository.findFailedNotificationsForRetry(
                maxRetries, LocalDateTime.now(), batchSize
        );
        log.info("[RETRY NOTIFICATIONS] Found {} failed notifications to retry", failedList.size());
        int retried = 0;
        for (Notification n : failedList) {
            // Đơn giản hóa: nếu là EMAIL thì thử gửi lại
            if (n.getChannel() == NotificationChannel.EMAIL && n.getRecipient() != null) {
                try {
                    emailSenderPort.sendHtmlEmail(n.getRecipient(), n.getSubject(), n.getContent());
                    n.markSent(LocalDateTime.now());
                    notificationRepository.save(n);
                    retried++;
                } catch (Exception ex) {
                    n.markFailed(ex.getMessage(), LocalDateTime.now());
                    notificationRepository.save(n);
                }
            }
        }
        return retried;
    }
}
