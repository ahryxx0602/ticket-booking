package com.ticket.notification.application.usecase;

import com.ticket.notification.application.dto.TelegramInboundUpdateDto;

public interface ProcessTelegramUpdateUseCase {
    void execute(TelegramInboundUpdateDto update);
}
