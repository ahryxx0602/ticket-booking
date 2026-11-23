package com.ticket.notification.application.usecase;

import com.ticket.notification.application.dto.RegisterDeviceTokenCommand;

public interface RegisterDeviceTokenUseCase {
    void execute(RegisterDeviceTokenCommand command);
}
