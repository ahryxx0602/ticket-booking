package com.ticket.notification.domain.model;

import com.ticket.shared.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification extends BaseEntity {
    private Long id;
    private Long userId;
    private String recipient;
    private NotificationChannel channel;
    private NotificationType type;
    private NotificationStatus status;
    private String subject;
    private String content;
    private String metadataJson;
    private String idempotencyKey;
    private String errorMessage;
    private LocalDateTime sentAt;
    @Builder.Default
    private int retryCount = 0;

    public void markSent(LocalDateTime now) {
        this.status = NotificationStatus.SENT;
        this.sentAt = now != null ? now : LocalDateTime.now();
        this.errorMessage = null;
    }

    public void markFailed(String error, LocalDateTime now) {
        this.status = NotificationStatus.FAILED;
        this.errorMessage = error != null && error.length() > 500 ? error.substring(0, 500) : error;
        this.retryCount++;
    }

    public boolean canRetry(int maxRetries) {
        return this.status == NotificationStatus.FAILED && this.retryCount < maxRetries;
    }
}
