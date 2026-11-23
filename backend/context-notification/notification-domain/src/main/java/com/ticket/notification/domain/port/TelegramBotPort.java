package com.ticket.notification.domain.port;

public interface TelegramBotPort {
    void sendMessage(Long chatId, String text);
    void sendAlertToAdmins(String alertText);
    void sendAlertToOperators(Long operatorId, String alertText);
}
