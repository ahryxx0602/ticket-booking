package com.ticket.notification.application.dto;

public record TelegramInboundUpdateDto(
        Long updateId,
        Long chatId,
        String messageText,
        String fromUsername,
        Long fromId
) {}
