package com.ticket.identity.application.usecase;

import com.ticket.identity.application.dto.AdminResetPasswordRequest;

public interface AdminResetPasswordUseCase {
    void execute(Long id, AdminResetPasswordRequest request);
}
