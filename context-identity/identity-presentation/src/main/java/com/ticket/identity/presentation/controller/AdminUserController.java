package com.ticket.identity.presentation.controller;

import com.ticket.identity.application.dto.AdminResetPasswordRequest;
import com.ticket.identity.application.dto.ChangeUserRoleRequest;
import com.ticket.identity.application.dto.UpdateUserStatusRequest;
import com.ticket.identity.application.dto.UserDetailResponse;
import com.ticket.identity.application.usecase.*;
import com.ticket.identity.domain.model.UserStatus;
import com.ticket.shared.dto.ApiResponse;
import com.ticket.shared.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminGetUsersUseCase getUsersUseCase;
    private final AdminGetUserDetailUseCase getUserDetailUseCase;
    private final AdminUpdateUserStatusUseCase updateUserStatusUseCase;
    private final AdminChangeUserRoleUseCase changeUserRoleUseCase;
    private final AdminResetPasswordUseCase resetPasswordUseCase;

    @GetMapping
    public ApiResponse<PageResponse<UserDetailResponse>> getUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<UserDetailResponse> response = getUsersUseCase.execute(keyword, role, status, page, size);
        return ApiResponse.ok("Users retrieved successfully", response);
    }

    @GetMapping("/{id}")
    public ApiResponse<UserDetailResponse> getUserDetail(@PathVariable Long id) {
        UserDetailResponse response = getUserDetailUseCase.execute(id);
        return ApiResponse.ok("User details retrieved successfully", response);
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<UserDetailResponse> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request,
            Principal principal) {
        String adminUsername = principal != null ? principal.getName() : "admin";
        UserDetailResponse response = updateUserStatusUseCase.execute(id, request, adminUsername);
        return ApiResponse.ok("User status updated successfully", response);
    }

    @PatchMapping("/{id}/role")
    public ApiResponse<UserDetailResponse> changeUserRole(
            @PathVariable Long id,
            @Valid @RequestBody ChangeUserRoleRequest request,
            Principal principal) {
        String adminUsername = principal != null ? principal.getName() : "admin";
        UserDetailResponse response = changeUserRoleUseCase.execute(id, request, adminUsername);
        return ApiResponse.ok("User role updated successfully", response);
    }

    @PostMapping("/{id}/reset-password")
    public ApiResponse<Void> resetPassword(
            @PathVariable Long id,
            @Valid @RequestBody AdminResetPasswordRequest request) {
        resetPasswordUseCase.execute(id, request);
        return ApiResponse.ok("User password reset successfully", null);
    }
}
