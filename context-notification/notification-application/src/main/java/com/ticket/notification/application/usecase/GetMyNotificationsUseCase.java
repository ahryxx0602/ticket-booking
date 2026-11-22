package com.ticket.notification.application.usecase;

import com.ticket.notification.application.dto.NotificationResponse;
import com.ticket.shared.dto.PageResponse;

public interface GetMyNotificationsUseCase {
    PageResponse<NotificationResponse> execute(Long userId, int page, int size);
}
