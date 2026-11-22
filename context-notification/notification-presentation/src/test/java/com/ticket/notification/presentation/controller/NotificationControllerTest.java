package com.ticket.notification.presentation.controller;

import com.ticket.notification.application.dto.GenerateLinkTokenResponse;
import com.ticket.notification.application.dto.NotificationResponse;
import com.ticket.notification.application.dto.RegisterDeviceTokenCommand;
import com.ticket.notification.application.usecase.GenerateTelegramLinkTokenUseCase;
import com.ticket.notification.application.usecase.GetMyNotificationsUseCase;
import com.ticket.notification.application.usecase.RegisterDeviceTokenUseCase;
import com.ticket.notification.application.usecase.RevokeDeviceTokenUseCase;
import com.ticket.shared.dto.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Notification Controller Unit Tests")
class NotificationControllerTest {

    @Mock private RegisterDeviceTokenUseCase registerDeviceTokenUseCase;
    @Mock private RevokeDeviceTokenUseCase revokeDeviceTokenUseCase;
    @Mock private GenerateTelegramLinkTokenUseCase generateTelegramLinkTokenUseCase;
    @Mock private GetMyNotificationsUseCase getMyNotificationsUseCase;
    @Mock private com.ticket.notification.domain.port.NotificationIdentityPort notificationIdentityPort;

    @InjectMocks private NotificationController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        controller.setUserIdResolver(principal -> 1L);
    }

    @Test
    @DisplayName("POST /devices should register device token and return 200")
    void testRegisterDeviceToken() throws Exception {
        String json = """
                {
                    "fcmToken": "fcm_token_sample_123",
                    "deviceType": "WEB"
                }
                """;

        mockMvc.perform(post("/api/v1/notifications/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .principal((Principal) () -> "test_user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(registerDeviceTokenUseCase).execute(any(RegisterDeviceTokenCommand.class));
    }

    @Test
    @DisplayName("DELETE /devices should revoke device token and return 200")
    void testRevokeDeviceToken() throws Exception {
        mockMvc.perform(delete("/api/v1/notifications/devices")
                        .param("fcmToken", "fcm_token_to_remove")
                        .principal((Principal) () -> "test_user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(revokeDeviceTokenUseCase).execute(eq(1L), eq("fcm_token_to_remove"));
    }

    @Test
    @DisplayName("POST /telegram/link-token should generate link token successfully")
    void testGenerateTelegramLinkToken() throws Exception {
        GenerateLinkTokenResponse tokenResp = new GenerateLinkTokenResponse(
                "TOKEN123",
                LocalDateTime.now().plusMinutes(10),
                "@TicketBookingBot",
                "Instruction"
        );
        when(generateTelegramLinkTokenUseCase.execute(1L)).thenReturn(tokenResp);

        mockMvc.perform(post("/api/v1/notifications/telegram/link-token")
                        .principal((Principal) () -> "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("TOKEN123"));
    }

    @Test
    @DisplayName("GET /me should return paginated notifications")
    void testGetMyNotifications() throws Exception {
        NotificationResponse resp = new NotificationResponse(
                100L, 1L, "user@test.com", "EMAIL", "PAYMENT_SUCCESS", "SENT", "Subj", "Cont", LocalDateTime.now(), LocalDateTime.now()
        );
        PageResponse<NotificationResponse> page = PageResponse.of(List.of(resp), 0, 10, 1);
        when(getMyNotificationsUseCase.execute(1L, 0, 10)).thenReturn(page);

        mockMvc.perform(get("/api/v1/notifications/me")
                        .principal((Principal) () -> "user")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }
}
