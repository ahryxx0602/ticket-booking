package com.ticket.notification.infrastructure.telegram;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.notification.telegram")
public class TelegramBotProperties {
    private boolean enabled = false;
    private String botToken = "dummy_telegram_bot_token";
    private String botUsername = "TicketBookingBot";
    private String webhookSecret = "telegram_webhook_secret_key";
    private Long adminChatId;
    private String mode = "polling"; // "polling" hoặc "webhook"
}
