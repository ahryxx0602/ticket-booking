package com.ticket.notification.presentation.controller;

import com.ticket.notification.application.dto.GenerateLinkTokenResponse;
import com.ticket.notification.application.dto.NotificationResponse;
import com.ticket.notification.application.dto.RegisterDeviceTokenCommand;
import com.ticket.notification.application.usecase.GenerateTelegramLinkTokenUseCase;
import com.ticket.notification.application.usecase.GetMyNotificationsUseCase;
import com.ticket.notification.application.usecase.RegisterDeviceTokenUseCase;
import com.ticket.notification.application.usecase.RevokeDeviceTokenUseCase;
import com.ticket.notification.domain.port.NotificationIdentityPort;
import com.ticket.shared.dto.ApiResponse;
import com.ticket.shared.dto.PageResponse;
import com.ticket.shared.exception.ForbiddenException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final RegisterDeviceTokenUseCase registerDeviceTokenUseCase;
    private final RevokeDeviceTokenUseCase revokeDeviceTokenUseCase;
    private final GenerateTelegramLinkTokenUseCase generateTelegramLinkTokenUseCase;
    private final GetMyNotificationsUseCase getMyNotificationsUseCase;
    private final NotificationIdentityPort notificationIdentityPort;

    public interface UserIdResolver {
        Long resolveUserId(Principal principal);
    }
    private UserIdResolver userIdResolver;

    public void setUserIdResolver(UserIdResolver resolver) {
        this.userIdResolver = resolver;
    }

    private Long getUserId(Principal principal) {
        if (principal == null) {
            throw new ForbiddenException("UNAUTHORIZED", "User must be authenticated");
        }
        if (userIdResolver != null) {
            return userIdResolver.resolveUserId(principal);
        }
        if (notificationIdentityPort != null) {
            return notificationIdentityPort.getUserProfileByUsername(principal.getName())
                    .map(NotificationIdentityPort.UserProfileSnapshot::userId)
                    .orElseThrow(() -> new ForbiddenException("USER_NOT_FOUND", "User profile not found for: " + principal.getName()));
        }
        try {
            return Long.parseLong(principal.getName());
        } catch (NumberFormatException ex) {
            throw new ForbiddenException("USER_NOT_FOUND", "Cannot resolve userId from principal: " + principal.getName());
        }
    }

    @PostMapping("/devices")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','OPERATOR')")
    public ResponseEntity<ApiResponse<Void>> registerDevice(
            @Valid @RequestBody RegisterDeviceTokenRequest request,
            Principal principal
    ) {
        Long userId = getUserId(principal);
        RegisterDeviceTokenCommand command = RegisterDeviceTokenCommand.builder()
                .userId(userId)
                .fcmToken(request.fcmToken())
                .deviceType(request.deviceType())
                .build();

        registerDeviceTokenUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.ok("Device token registered successfully", null));
    }

    @DeleteMapping("/devices")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','OPERATOR')")
    public ResponseEntity<ApiResponse<Void>> revokeDevice(
            @RequestParam("fcmToken") String fcmToken,
            Principal principal
    ) {
        Long userId = getUserId(principal);
        revokeDeviceTokenUseCase.execute(userId, fcmToken);
        return ResponseEntity.ok(ApiResponse.ok("Device token revoked successfully", null));
    }

    @PostMapping("/telegram/link-token")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    public ResponseEntity<ApiResponse<GenerateLinkTokenResponse>> generateTelegramLinkToken(Principal principal) {
        Long userId = getUserId(principal);
        GenerateLinkTokenResponse response = generateTelegramLinkTokenUseCase.execute(userId);
        return ResponseEntity.ok(ApiResponse.ok("Telegram link token generated successfully", response));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN','OPERATOR')")
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getMyNotifications(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            Principal principal
    ) {
        Long userId = getUserId(principal);
        PageResponse<NotificationResponse> result = getMyNotificationsUseCase.execute(userId, page, size);
        return ResponseEntity.ok(ApiResponse.ok("Notifications retrieved successfully", result));
    }

    public record RegisterDeviceTokenRequest(String fcmToken, String deviceType) {}
}
