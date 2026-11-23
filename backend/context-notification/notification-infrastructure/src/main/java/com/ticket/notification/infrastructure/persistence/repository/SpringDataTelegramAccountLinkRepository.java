package com.ticket.notification.infrastructure.persistence.repository;

import com.ticket.notification.infrastructure.persistence.entity.TelegramAccountLinkJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpringDataTelegramAccountLinkRepository extends JpaRepository<TelegramAccountLinkJpaEntity, Long> {
    Optional<TelegramAccountLinkJpaEntity> findByTelegramChatId(Long telegramChatId);
    Optional<TelegramAccountLinkJpaEntity> findByUserId(Long userId);
    List<TelegramAccountLinkJpaEntity> findByRoleAndActiveTrue(String role);
    void deleteByTelegramChatId(Long telegramChatId);
}
