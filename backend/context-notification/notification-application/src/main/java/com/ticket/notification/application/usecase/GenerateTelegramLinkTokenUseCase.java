package com.ticket.notification.application.usecase;

import com.ticket.notification.application.dto.GenerateLinkTokenResponse;

public interface GenerateTelegramLinkTokenUseCase {
    GenerateLinkTokenResponse execute(Long userId);
}
