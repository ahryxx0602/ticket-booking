package com.ticket.identity.application.usecase;

import com.ticket.identity.application.dto.AuthRequest;
import com.ticket.identity.application.dto.AuthResponse;

public interface LoginUseCase {
    AuthResponse execute(AuthRequest request);
}
