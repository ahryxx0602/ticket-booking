package com.ticket.identity.presentation.controller;

import com.ticket.identity.application.dto.AuthRequest;
import com.ticket.identity.application.dto.AuthResponse;
import com.ticket.identity.application.dto.RegisterRequest;
import com.ticket.identity.application.usecase.LoginUseCase;
import com.ticket.identity.application.usecase.RegisterUseCase;
import com.ticket.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final RegisterUseCase registerUseCase;

    @GetMapping
    public ResponseEntity<ApiResponse<java.util.Map<String, String>>> info() {
        return ResponseEntity.ok(ApiResponse.ok("Auth Service is operational", java.util.Map.of(
                "register", "POST /api/v1/auth/register",
                "login", "POST /api/v1/auth/login"
        )));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody AuthRequest request) {
        AuthResponse response = loginUseCase.execute(request);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = registerUseCase.execute(request);
        return ResponseEntity.ok(ApiResponse.ok("Registration successful", response));
    }
}
