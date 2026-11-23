package com.ticket.identity.application.usecase;

import com.ticket.identity.application.dto.ChangeUserRoleRequest;
import com.ticket.identity.application.dto.UserDetailResponse;

public interface AdminChangeUserRoleUseCase {
    UserDetailResponse execute(Long id, ChangeUserRoleRequest request, String currentAdminUsername);
}
