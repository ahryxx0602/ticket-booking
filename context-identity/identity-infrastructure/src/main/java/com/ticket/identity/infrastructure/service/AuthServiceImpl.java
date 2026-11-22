package com.ticket.identity.infrastructure.service;

import com.ticket.identity.application.dto.AuthRequest;
import com.ticket.identity.application.dto.AuthResponse;
import com.ticket.identity.application.dto.RegisterRequest;
import com.ticket.identity.application.usecase.LoginUseCase;
import com.ticket.identity.application.usecase.RegisterUseCase;
import com.ticket.identity.domain.model.User;
import com.ticket.identity.domain.model.UserStatus;
import com.ticket.identity.domain.repository.UserRepository;
import com.ticket.identity.infrastructure.security.JwtTokenProvider;
import com.ticket.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements LoginUseCase, RegisterUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

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

        String roleName = user.getRole() != null ? user.getRole().name() : "ROLE_CUSTOMER";
        String token = tokenProvider.generateToken(user.getUsername(), roleName);
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(86400000L)
                .username(user.getUsername())
                .role(roleName)
                .build();
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
                .role(com.ticket.identity.domain.model.UserRole.ROLE_CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();

        User saved = userRepository.save(user);
        String roleName = saved.getRole() != null ? saved.getRole().name() : "ROLE_CUSTOMER";
        String token = tokenProvider.generateToken(saved.getUsername(), roleName);

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(86400000L)
                .username(saved.getUsername())
                .role(roleName)
                .build();
    }
}
