package com.ticket.identity.application.usecase;

import com.ticket.identity.application.dto.AuthResponse;
import com.ticket.identity.application.dto.RegisterRequest;

public interface RegisterUseCase {
    AuthResponse execute(RegisterRequest request);
}
