package com.ticket.notification.presentation.controller;

import com.ticket.notification.application.usecase.ProcessTelegramUpdateUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Telegram Webhook Controller Tests")
class TelegramWebhookControllerTest {

    @Mock private ProcessTelegramUpdateUseCase processTelegramUpdateUseCase;
    @InjectMocks private TelegramWebhookController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        ReflectionTestUtils.setField(controller, "configuredSecret", "MY_SUPER_SECRET_123");
    }

    @Test
    @DisplayName("Valid secret token should accept webhook and invoke usecase")
    void testTelegramWebhook_ValidSecret() throws Exception {
        String updateJson = """
                {
                    "update_id": 10001,
                    "message": {
                        "chat": { "id": 998877 },
                        "text": "/start",
                        "from": { "id": 998877, "username": "sample_user" }
                    }
                }
                """;

        mockMvc.perform(post("/api/v1/notifications/telegram/webhook")
                        .header("X-Telegram-Bot-Api-Secret-Token", "MY_SUPER_SECRET_123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));

        verify(processTelegramUpdateUseCase).execute(any());
    }

    @Test
    @DisplayName("Invalid secret token should reject webhook with 403 Forbidden")
    void testTelegramWebhook_InvalidSecret() throws Exception {
        String updateJson = "{\"update_id\": 10001}";

        mockMvc.perform(post("/api/v1/notifications/telegram/webhook")
                        .header("X-Telegram-Bot-Api-Secret-Token", "WRONG_SECRET")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        verify(processTelegramUpdateUseCase, never()).execute(any());
    }
}
