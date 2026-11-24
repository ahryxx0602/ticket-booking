package com.ticket.identity.application.usecase;

import com.ticket.identity.application.dto.AuthResponse;

/**
 * FE-SEC-002: Truy vấn thông tin hồ sơ & tập quyền RBAC mới nhất từ DB cho phiên đang đăng nhập.
 */
public interface GetCurrentUserProfileUseCase {
    AuthResponse getCurrentProfile(String username);
}
