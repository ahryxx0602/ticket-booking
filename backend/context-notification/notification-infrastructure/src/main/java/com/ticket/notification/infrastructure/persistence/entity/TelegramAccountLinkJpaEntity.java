package com.ticket.notification.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "telegram_account_links", indexes = {
        @Index(name = "idx_tele_user", columnList = "user_id"),
        @Index(name = "idx_tele_chat", columnList = "telegram_chat_id")
})
@Getter
@Setter
public class TelegramAccountLinkJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "telegram_chat_id", nullable = false, unique = true)
    private Long telegramChatId;

    @Column(nullable = false, length = 30)
    private String role;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "linked_at", nullable = false)
    private LocalDateTime linkedAt;

    @PrePersist
    public void prePersist() {
        if (this.linkedAt == null) {
            this.linkedAt = LocalDateTime.now();
        }
    }
}
