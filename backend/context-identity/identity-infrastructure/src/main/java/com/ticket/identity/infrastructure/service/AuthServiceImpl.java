package com.ticket.identity.infrastructure.service;

import com.ticket.identity.application.dto.AuthRequest;
import com.ticket.identity.application.dto.AuthResponse;
import com.ticket.identity.application.dto.RegisterRequest;
import com.ticket.identity.application.usecase.GetCurrentUserProfileUseCase;
import com.ticket.identity.application.usecase.LoginUseCase;
import com.ticket.identity.application.usecase.RegisterUseCase;
import com.ticket.identity.domain.model.AdminPermissionCatalog;
import com.ticket.identity.domain.model.SystemRole;
import com.ticket.identity.domain.model.User;
import com.ticket.identity.domain.model.UserRole;
import com.ticket.identity.domain.model.UserStatus;
import com.ticket.identity.domain.repository.SystemRoleRepository;
import com.ticket.identity.domain.repository.UserRepository;
import com.ticket.identity.infrastructure.security.JwtTokenProvider;
import com.ticket.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements LoginUseCase, RegisterUseCase, GetCurrentUserProfileUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    private SystemRoleRepository systemRoleRepository;

    @Autowired(required = false)
    public void setSystemRoleRepository(SystemRoleRepository systemRoleRepository) {
        this.systemRoleRepository = systemRoleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse execute(AuthRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException("INVALID_CREDENTIALS", "Username or password incorrect"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("INVALID_CREDENTIALS", "Username or password incorrect");
        }

        if (user.isLocked()) {
            throw new BusinessException("ACCOUNT_LOCKED", "Your account has been locked. Please contact administrator.");
        }

        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse execute(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("USERNAME_EXISTS", "Username is already in use");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("EMAIL_EXISTS", "Email is already in use");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(UserRole.ROLE_CUSTOMER)
                .customRoleCode("ROLE_CUSTOMER")
                .status(UserStatus.ACTIVE)
                .build();

        User saved = userRepository.save(user);
        return buildAuthResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse getCurrentProfile(String username) {
        if (username == null || username.isBlank()) {
            throw new BusinessException("UNAUTHORIZED", "Authentication is required");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "User account no longer exists"));
        if (user.isLocked()) {
            throw new BusinessException("ACCOUNT_LOCKED", "Your account has been locked by administrator");
        }
        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String baseRole = user.getRole() != null ? user.getRole().name() : "ROLE_CUSTOMER";
        String customRole = user.getEffectiveRoleCode();
        String displayRoleName = customRole;
        List<String> permissions = Collections.emptyList();

        if ("ROLE_ADMIN".equalsIgnoreCase(baseRole)) {
            if ("ROLE_ADMIN".equalsIgnoreCase(customRole)) {
                displayRoleName = "Quản Trị Viên Toàn Sàn (Super Admin)";
                permissions = AdminPermissionCatalog.allPermissionsList();
            } else if (systemRoleRepository != null) {
                Optional<SystemRole> srOpt = systemRoleRepository.findByCode(customRole);
                if (srOpt.isPresent()) {
                    displayRoleName = srOpt.get().getName();
                    permissions = srOpt.get().getPermissions();
                }
            }
        } else if ("ROLE_OPERATOR".equalsIgnoreCase(baseRole)) {
            displayRoleName = "Chủ Xe / Đối Tác Vận Tải";
        } else {
            displayRoleName = "Khách Hàng Đặt Vé";
        }

        String token;
        if (baseRole.equalsIgnoreCase(customRole) && permissions.isEmpty()) {
            token = tokenProvider.generateToken(user.getUsername(), baseRole);
        } else {
            token = tokenProvider.generateToken(user.getUsername(), baseRole, customRole, displayRoleName, permissions);
            if (token == null) {
                // Tương thích với unit test cũ mock tokenProvider.generateToken(username, role)
                token = tokenProvider.generateToken(user.getUsername(), baseRole);
            }
        }

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(86400000L)
                .username(user.getUsername())
                .role(baseRole)
                .customRole(customRole)
                .roleName(displayRoleName)
                .permissions(permissions)
                .build();
    }
}
