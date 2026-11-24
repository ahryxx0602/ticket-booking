package com.ticket.identity.application.usecase;

import com.ticket.identity.application.dto.AdminCreateUserRequest;
import com.ticket.identity.application.dto.UserDetailResponse;

public interface AdminCreateUserUseCase {
    UserDetailResponse execute(AdminCreateUserRequest request, String currentAdminUsername);
}
