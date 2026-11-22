package com.ticket.notification.infrastructure.telegram;

import com.ticket.notification.domain.model.TelegramAccountLink;
import com.ticket.notification.domain.port.TelegramAccountLinkRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Telegram Bot Adapter Tests")
class TelegramBotAdapterTest {

    @Mock
    private TelegramAccountLinkRepository telegramAccountLinkRepository;

    @Test
    @DisplayName("When Telegram is disabled, sendMessage should log mock without throwing exception")
    void testSendMessage_DisabledMode() {
        TelegramBotProperties properties = new TelegramBotProperties();
        properties.setEnabled(false);

        TelegramBotAdapter adapter = new TelegramBotAdapter(properties, telegramAccountLinkRepository);

        assertDoesNotThrow(() -> adapter.sendMessage(123456L, "Test Message"));
        assertDoesNotThrow(() -> adapter.sendMessage(null, "Test Message"));
    }

    @Test
    @DisplayName("Send alert to admins should query active links and send messages")
    void testSendAlertToAdmins_DisabledMode() {
        TelegramBotProperties properties = new TelegramBotProperties();
        properties.setEnabled(false);
        properties.setAdminChatId(999L);

        TelegramAccountLink link = TelegramAccountLink.builder()
                .userId(1L)
                .telegramChatId(888L)
                .role("ROLE_ADMIN")
                .active(true)
                .linkedAt(LocalDateTime.now())
                .build();

        when(telegramAccountLinkRepository.findByRoleAndActiveTrue("ROLE_ADMIN")).thenReturn(List.of(link));

        TelegramBotAdapter adapter = new TelegramBotAdapter(properties, telegramAccountLinkRepository);

        assertDoesNotThrow(() -> adapter.sendAlertToAdmins("Urgent Alert!"));
        verify(telegramAccountLinkRepository).findByRoleAndActiveTrue("ROLE_ADMIN");
    }
}
