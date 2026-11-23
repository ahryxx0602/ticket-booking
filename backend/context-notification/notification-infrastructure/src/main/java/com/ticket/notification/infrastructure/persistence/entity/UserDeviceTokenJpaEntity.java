package com.ticket.notification.infrastructure.persistence.entity;

import com.ticket.notification.domain.model.DeviceType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_device_tokens", indexes = {
        @Index(name = "idx_user_tokens", columnList = "user_id"),
        @Index(name = "idx_fcm_token", columnList = "fcm_token")
})
@Getter
@Setter
public class UserDeviceTokenJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "fcm_token", nullable = false, unique = true, length = 512)
    private String fcmToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false, length = 20)
    private DeviceType deviceType;

    @Column(name = "last_active_at", nullable = false)
    private LocalDateTime lastActiveAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.lastActiveAt == null) {
            this.lastActiveAt = LocalDateTime.now();
        }
    }
}
