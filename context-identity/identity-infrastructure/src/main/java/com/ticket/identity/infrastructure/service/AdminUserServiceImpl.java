package com.ticket.identity.infrastructure.service;

import com.ticket.identity.application.dto.AdminResetPasswordRequest;
import com.ticket.identity.application.dto.ChangeUserRoleRequest;
import com.ticket.identity.application.dto.UpdateUserStatusRequest;
import com.ticket.identity.application.dto.UserDetailResponse;
import com.ticket.identity.application.usecase.*;
import com.ticket.identity.domain.model.User;
import com.ticket.identity.domain.model.UserRole;
import com.ticket.identity.domain.model.UserStatus;
import com.ticket.identity.domain.repository.UserRepository;
import com.ticket.shared.dto.PageResponse;
import com.ticket.shared.exception.BusinessException;
import com.ticket.shared.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements
        AdminGetUsersUseCase,
        AdminGetUserDetailUseCase,
        AdminUpdateUserStatusUseCase,
        AdminChangeUserRoleUseCase,
        AdminResetPasswordUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserDetailResponse> execute(String keyword, String role, UserStatus status, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = size <= 0 ? 10 : size;

        List<User> users = userRepository.searchUsers(keyword, role, status, safePage, safeSize);
        long total = userRepository.countUsers(keyword, role, status);

        List<UserDetailResponse> responses = users.stream()
                .map(this::toResponse)
                .toList();

        return PageResponse.of(responses, safePage, safeSize, total);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetailResponse execute(Long id) {
        User user = findUserOrThrow(id);
        return toResponse(user);
    }

    @Override
    @Transactional
    public UserDetailResponse execute(Long id, UpdateUserStatusRequest request, String currentAdminUsername) {
        User user = findUserOrThrow(id);

        if (user.getUsername().equalsIgnoreCase(currentAdminUsername) && request.getStatus() == UserStatus.LOCKED) {
            throw new BusinessException("ADMIN_CANNOT_LOCK_SELF", "You cannot lock your own administrator account");
        }

        if (request.getStatus() == UserStatus.LOCKED) {
            user.lock();
            log.info("[ADMIN USER] User {} has been LOCKED by admin {}", user.getUsername(), currentAdminUsername);
        } else if (request.getStatus() == UserStatus.ACTIVE) {
            user.activate();
            log.info("[ADMIN USER] User {} has been ACTIVATED by admin {}", user.getUsername(), currentAdminUsername);
        } else {
            user.setStatus(request.getStatus());
        }

        User updated = userRepository.save(user);
        return toResponse(updated);
    }

    @Override
    @Transactional
    public UserDetailResponse execute(Long id, ChangeUserRoleRequest request, String currentAdminUsername) {
        User user = findUserOrThrow(id);

        if (user.getUsername().equalsIgnoreCase(currentAdminUsername)) {
            throw new BusinessException("ADMIN_CANNOT_DEMOTE_SELF", "You cannot modify role of your own account");
        }

        UserRole newRole = UserRole.fromString(request.getRole());
        user.changeRole(newRole);
        User updated = userRepository.save(user);
        log.info("[ADMIN USER] Role of user {} changed to {} by admin {}", user.getUsername(), newRole, currentAdminUsername);
        return toResponse(updated);
    }

    @Override
    @Transactional
    public void execute(Long id, AdminResetPasswordRequest request) {
        User user = findUserOrThrow(id);
        user.updatePassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("[ADMIN USER] Password of user {} reset successfully by admin", user.getUsername());
    }

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User", id));
    }

    private UserDetailResponse toResponse(User user) {
        return UserDetailResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole() != null ? user.getRole().name() : "ROLE_CUSTOMER")
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
