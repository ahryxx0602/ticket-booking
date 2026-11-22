package com.ticket.notification.domain.port;

import com.ticket.notification.domain.model.TelegramAccountLink;

import java.util.List;
import java.util.Optional;

public interface TelegramAccountLinkRepository {
    TelegramAccountLink save(TelegramAccountLink link);
    Optional<TelegramAccountLink> findByTelegramChatId(Long telegramChatId);
    Optional<TelegramAccountLink> findByUserId(Long userId);
    List<TelegramAccountLink> findByRoleAndActiveTrue(String role);
    void deleteByTelegramChatId(Long telegramChatId);
}
