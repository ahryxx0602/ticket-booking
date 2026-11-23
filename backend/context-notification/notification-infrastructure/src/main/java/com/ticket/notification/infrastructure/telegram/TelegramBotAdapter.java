package com.ticket.notification.infrastructure.telegram;

import com.ticket.notification.domain.model.TelegramAccountLink;
import com.ticket.notification.domain.port.TelegramAccountLinkRepository;
import com.ticket.notification.domain.port.TelegramBotPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramBotAdapter implements TelegramBotPort {

    private static final int MAX_MESSAGE_LENGTH = 3500;
    private static final String TELEGRAM_API_BASE = "https://api.telegram.org/bot";

    private final TelegramBotProperties properties;
    private final TelegramAccountLinkRepository telegramAccountLinkRepository;

    private RestClient restClient;

    private RestClient getRestClient() {
        if (this.restClient == null) {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(Duration.ofSeconds(3));
            factory.setReadTimeout(Duration.ofSeconds(5));
            this.restClient = RestClient.builder()
                    .requestFactory(factory)
                    .build();
        }
        return this.restClient;
    }

    @Override
    public void sendMessage(Long chatId, String text) {
        if (chatId == null || text == null || text.trim().isEmpty()) {
            return;
        }

        if (!properties.isEnabled()) {
            log.info("[MOCK TELEGRAM] Telegram disabled. Mock sendMessage to chatId: {}\nContent:\n{}", chatId, text);
            return;
        }

        // Tự động chia nhỏ tin nhắn nếu dài hơn 3500 ký tự (chống trần 4096 ký tự của Telegram)
        if (text.length() <= MAX_MESSAGE_LENGTH) {
            sendSingleMessage(chatId, text);
        } else {
            int start = 0;
            while (start < text.length()) {
                int end = Math.min(start + MAX_MESSAGE_LENGTH, text.length());
                sendSingleMessage(chatId, text.substring(start, end));
                start = end;
            }
        }
    }

    private void sendSingleMessage(Long chatId, String text) {
        try {
            String url = TELEGRAM_API_BASE + properties.getBotToken() + "/sendMessage";
            Map<String, Object> body = Map.of(
                    "chat_id", chatId,
                    "text", text
            );

            getRestClient().post()
                    .uri(url)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.info("[TELEGRAM SENT] Sent message to chatId: {}", chatId);
        } catch (Exception ex) {
            log.error("[TELEGRAM ERROR] Failed to send message to chatId: {}", chatId, ex);
        }
    }

    @Override
    public void sendAlertToAdmins(String alertText) {
        if (properties.getAdminChatId() != null) {
            sendMessage(properties.getAdminChatId(), alertText);
        }

        // Bắn cho tất cả Admin đã liên kết tài khoản
        List<TelegramAccountLink> adminLinks = telegramAccountLinkRepository.findByRoleAndActiveTrue("ROLE_ADMIN");
        for (TelegramAccountLink link : adminLinks) {
            if (!link.getTelegramChatId().equals(properties.getAdminChatId())) {
                sendMessage(link.getTelegramChatId(), alertText);
            }
        }
    }

    @Override
    public void sendAlertToOperators(Long operatorId, String alertText) {
        // Tìm tài khoản liên kết theo operatorId / role
        List<TelegramAccountLink> opLinks = telegramAccountLinkRepository.findByRoleAndActiveTrue("ROLE_OPERATOR");
        for (TelegramAccountLink link : opLinks) {
            sendMessage(link.getTelegramChatId(), alertText);
        }
    }
}
