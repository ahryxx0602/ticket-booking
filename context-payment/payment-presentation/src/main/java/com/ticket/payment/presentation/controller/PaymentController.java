package com.ticket.payment.presentation.controller;

import com.ticket.payment.application.dto.CreatePaymentCommand;
import com.ticket.payment.application.dto.PaymentInitResponse;
import com.ticket.payment.application.usecase.CreatePaymentUseCase;
import com.ticket.shared.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final CreatePaymentUseCase createPaymentUseCase;

    @PostMapping("/checkout")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<PaymentInitResponse>> createPayment(
            @Valid @RequestBody CreatePaymentCommand command,
            Principal principal,
            HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();
        String username = (principal != null) ? principal.getName() : null;
        PaymentInitResponse response = createPaymentUseCase.execute(command, username, clientIp);
        return ResponseEntity.ok(ApiResponse.ok("Payment link created successfully", response));
    }
}
