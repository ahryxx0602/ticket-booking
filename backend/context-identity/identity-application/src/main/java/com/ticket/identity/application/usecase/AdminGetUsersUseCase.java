package com.ticket.identity.application.usecase;

import com.ticket.identity.application.dto.UserDetailResponse;
import com.ticket.identity.domain.model.UserStatus;
import com.ticket.shared.dto.PageResponse;

public interface AdminGetUsersUseCase {
    PageResponse<UserDetailResponse> execute(String keyword, String role, UserStatus status, int page, int size);
}
