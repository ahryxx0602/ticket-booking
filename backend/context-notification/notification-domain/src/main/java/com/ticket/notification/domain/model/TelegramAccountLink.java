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
public class TelegramAccountLink extends BaseEntity {
    private Long id;
    private Long userId;
    private Long telegramChatId;
    private String role;
    @Builder.Default
    private boolean active = true;
    private LocalDateTime linkedAt;

    public void deactivate() {
        this.active = false;
    }

    public void activate(LocalDateTime now) {
        this.active = true;
        this.linkedAt = now != null ? now : LocalDateTime.now();
    }
}
