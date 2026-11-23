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
public class UserDeviceToken extends BaseEntity {
    private Long id;
    private Long userId;
    private String fcmToken;
    private DeviceType deviceType;
    private LocalDateTime lastActiveAt;

    public void refreshActiveAt(LocalDateTime now) {
        this.lastActiveAt = now != null ? now : LocalDateTime.now();
    }

    public void reassignToUser(Long newUserId, LocalDateTime now) {
        this.userId = newUserId;
        this.lastActiveAt = now != null ? now : LocalDateTime.now();
    }
}
