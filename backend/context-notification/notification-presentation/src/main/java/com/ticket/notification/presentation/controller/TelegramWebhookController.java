package com.ticket.notification.presentation.controller;

import com.ticket.notification.application.dto.TelegramInboundUpdateDto;
import com.ticket.notification.application.usecase.ProcessTelegramUpdateUseCase;
import com.ticket.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications/telegram")
@RequiredArgsConstructor
public class TelegramWebhookController {

    private final ProcessTelegramUpdateUseCase processTelegramUpdateUseCase;

    @Value("${app.notification.telegram.enabled:false}")
    private boolean telegramEnabled = true;

    @Value("${app.notification.telegram.webhook-secret:telegram_webhook_secret_key}")
    private String configuredSecret;

    @PostMapping("/webhook")
    public ResponseEntity<?> handleTelegramWebhook(
            @RequestHeader(name = "X-Telegram-Bot-Api-Secret-Token", required = false) String secretToken,
            @RequestBody Map<String, Object> updatePayload
    ) {
        // BE-SEC-009: Nếu tích hợp Telegram đang tắt, từ chối xử lý mọi request tới webhook
        if (!telegramEnabled) {
            log.debug("[TELEGRAM WEBHOOK] Ignored webhook request because Telegram integration is disabled");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error("TELEGRAM_DISABLED", "Telegram integration is disabled"));
        }

        // Xác thực Secret Token bảo mật chống Timing Attack (ADR-07)
        if (!isValidSecret(secretToken)) {
            log.warn("[TELEGRAM WEBHOOK] Unauthorized webhook request with invalid secret token");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("UNAUTHORIZED", "Invalid Telegram secret token"));
        }

        try {
            TelegramInboundUpdateDto dto = parseTelegramUpdate(updatePayload);
            if (dto != null) {
                processTelegramUpdateUseCase.execute(dto);
            }
        } catch (Exception ex) {
            log.error("[TELEGRAM WEBHOOK ERROR] Error processing telegram update", ex);
        }

        // Telegram yêu cầu webhook luôn trả về HTTP 200 OK để không retry liên tục
        return ResponseEntity.ok(Map.of("ok", true));
    }

    private boolean isValidSecret(String provided) {
        if (provided == null || provided.isBlank() || configuredSecret == null || configuredSecret.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(
                provided.getBytes(StandardCharsets.UTF_8),
                configuredSecret.getBytes(StandardCharsets.UTF_8)
        );
    }

    @SuppressWarnings("unchecked")
    private TelegramInboundUpdateDto parseTelegramUpdate(Map<String, Object> payload) {
        if (payload == null) return null;

        Long updateId = payload.get("update_id") instanceof Number n ? n.longValue() : null;
        Map<String, Object> message = (Map<String, Object>) payload.get("message");
        if (message == null) {
            return null;
        }

        Map<String, Object> chat = (Map<String, Object>) message.get("chat");
        Long chatId = chat != null && chat.get("id") instanceof Number n ? n.longValue() : null;
        String text = (String) message.get("text");

        Map<String, Object> from = (Map<String, Object>) message.get("from");
        String username = from != null ? (String) from.get("username") : null;
        Long fromId = from != null && from.get("id") instanceof Number n ? n.longValue() : null;

        return new TelegramInboundUpdateDto(updateId, chatId, text, username, fromId);
    }
}
