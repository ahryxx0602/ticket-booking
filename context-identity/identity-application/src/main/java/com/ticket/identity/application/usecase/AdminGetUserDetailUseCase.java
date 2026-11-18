package com.ticket.identity.application.usecase;

import com.ticket.identity.application.dto.UserDetailResponse;

public interface AdminGetUserDetailUseCase {
    UserDetailResponse execute(Long id);
}
