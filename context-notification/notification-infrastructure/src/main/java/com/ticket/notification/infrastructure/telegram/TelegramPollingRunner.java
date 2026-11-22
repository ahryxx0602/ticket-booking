package com.ticket.notification.infrastructure.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.notification.application.dto.TelegramInboundUpdateDto;
import com.ticket.notification.application.usecase.ProcessTelegramUpdateUseCase;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.notification.telegram.enabled", havingValue = "true")
public class TelegramPollingRunner {

    private final TelegramBotProperties properties;
    private final ProcessTelegramUpdateUseCase processTelegramUpdateUseCase;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final AtomicBoolean running = new AtomicBoolean(false);
    private ExecutorService pollingExecutor;
    private RestClient restClient;
    private long lastUpdateId = 0;

    @PostConstruct
    public void start() {
        if (!"polling".equalsIgnoreCase(properties.getMode())) {
            log.info("[TELEGRAM BOT] Running in WEBHOOK mode. Polling runner inactive.");
            return;
        }

        if (properties.getBotToken() == null || properties.getBotToken().contains("placeholder") || properties.getBotToken().contains("dummy")) {
            log.warn("[TELEGRAM BOT] Telegram is enabled, but bot-token is a placeholder. Polling runner skipped.");
            return;
        }

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(35)); // Lớn hơn timeout long-polling (20s)
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();

        running.set(true);
        // Sử dụng Virtual Thread để chạy vòng lặp long polling không tốn tài nguyên
        pollingExecutor = Executors.newVirtualThreadPerTaskExecutor();
        pollingExecutor.submit(this::pollLoop);

        log.info("[TELEGRAM BOT] Started Telegram Long Polling Runner for bot: @{}", properties.getBotUsername());
    }

    @PreDestroy
    public void stop() {
        running.set(false);
        if (pollingExecutor != null && !pollingExecutor.isShutdown()) {
            pollingExecutor.shutdownNow();
        }
        log.info("[TELEGRAM BOT] Stopped Telegram Long Polling Runner.");
    }

    private void pollLoop() {
        String baseUrl = "https://api.telegram.org/bot" + properties.getBotToken() + "/getUpdates";

        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                long offset = lastUpdateId + 1;
                String url = baseUrl + "?offset=" + offset + "&timeout=20";

                String response = restClient.get()
                        .uri(url)
                        .retrieve()
                        .body(String.class);

                if (response != null && !response.isEmpty()) {
                    JsonNode root = objectMapper.readTree(response);
                    if (root.path("ok").asBoolean()) {
                        JsonNode result = root.path("result");
                        if (result.isArray()) {
                            for (JsonNode updateNode : result) {
                                long updateId = updateNode.path("update_id").asLong();
                                if (updateId > lastUpdateId) {
                                    lastUpdateId = updateId;
                                }

                                TelegramInboundUpdateDto dto = parseNodeToDto(updateNode);
                                if (dto != null) {
                                    processTelegramUpdateUseCase.execute(dto);
                                }
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                if (!running.get()) {
                    break;
                }
                log.warn("[TELEGRAM POLLING] Transient error fetching updates: {}. Retrying in 5s...", ex.getMessage());
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private TelegramInboundUpdateDto parseNodeToDto(JsonNode updateNode) {
        long updateId = updateNode.path("update_id").asLong();
        JsonNode messageNode = updateNode.path("message");
        if (messageNode.isMissingNode() || messageNode.isNull()) {
            return null;
        }

        Long chatId = messageNode.path("chat").path("id").asLong();
        String text = messageNode.path("text").asText(null);
        String username = messageNode.path("from").path("username").asText(null);
        Long fromId = messageNode.path("from").path("id").asLong();

        return new TelegramInboundUpdateDto(updateId, chatId, text, username, fromId);
    }
}
