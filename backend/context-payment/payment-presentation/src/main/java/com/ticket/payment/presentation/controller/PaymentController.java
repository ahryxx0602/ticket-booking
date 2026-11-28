package com.ticket.payment.presentation.controller;

import com.ticket.payment.application.dto.CreatePaymentCommand;
import com.ticket.payment.application.dto.CustomerPaymentStatusResponse;
import com.ticket.payment.application.dto.PaymentInitResponse;
import com.ticket.payment.application.usecase.CreatePaymentUseCase;
import com.ticket.payment.application.usecase.GetCustomerPaymentStatusUseCase;
import com.ticket.payment.application.usecase.HandleVnPayBrowserReturnUseCase;
import com.ticket.shared.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final CreatePaymentUseCase createPaymentUseCase;
    private final GetCustomerPaymentStatusUseCase getCustomerPaymentStatusUseCase;
    private final HandleVnPayBrowserReturnUseCase handleVnPayBrowserReturnUseCase;

    @PostMapping("/checkout")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<PaymentInitResponse>> createPayment(
            @Valid @RequestBody CreatePaymentCommand command,
            Principal principal,
            HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();
        String username = requireUsername(principal);
        PaymentInitResponse response = createPaymentUseCase.execute(command, username, clientIp);
        return ResponseEntity.ok(ApiResponse.ok("Payment link created successfully", response));
    }

    @GetMapping("/bookings/{bookingReference}/status")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<CustomerPaymentStatusResponse>> getPaymentStatus(
            @PathVariable String bookingReference,
            Principal principal) {
        CustomerPaymentStatusResponse response = getCustomerPaymentStatusUseCase.execute(bookingReference, requireUsername(principal));
        return ResponseEntity.ok(ApiResponse.ok("Payment status retrieved successfully", response));
    }

    @GetMapping("/vnpay-return")
    public void handleVnPayBrowserReturn(
            @RequestParam Map<String, String> allParams,
            HttpServletResponse response) throws IOException {
        String redirectUrl = handleVnPayBrowserReturnUseCase.execute(allParams);
        response.sendRedirect(redirectUrl);
    }

    private String requireUsername(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new AccessDeniedException("Authentication is required");
        }
        return principal.getName();
    }
}
