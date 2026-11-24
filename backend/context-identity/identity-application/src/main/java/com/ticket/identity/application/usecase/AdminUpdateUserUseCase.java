package com.ticket.identity.application.usecase;

import com.ticket.identity.application.dto.AdminUpdateUserRequest;
import com.ticket.identity.application.dto.UserDetailResponse;

public interface AdminUpdateUserUseCase {
    UserDetailResponse execute(Long id, AdminUpdateUserRequest request, String currentAdminUsername);
}
