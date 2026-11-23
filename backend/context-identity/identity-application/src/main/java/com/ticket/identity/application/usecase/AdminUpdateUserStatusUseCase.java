package com.ticket.identity.application.usecase;

import com.ticket.identity.application.dto.UpdateUserStatusRequest;
import com.ticket.identity.application.dto.UserDetailResponse;

public interface AdminUpdateUserStatusUseCase {
    UserDetailResponse execute(Long id, UpdateUserStatusRequest request, String currentAdminUsername);
}
