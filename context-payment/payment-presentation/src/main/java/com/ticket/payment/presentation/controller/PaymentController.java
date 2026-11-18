package com.ticket.payment.presentation.controller;

import com.ticket.payment.application.dto.CreatePaymentCommand;
import com.ticket.payment.application.dto.PaymentInitResponse;
import com.ticket.payment.application.usecase.CreatePaymentUseCase;
import com.ticket.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final CreatePaymentUseCase createPaymentUseCase;

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<PaymentInitResponse>> createPayment(@Valid @RequestBody CreatePaymentCommand command) {
        PaymentInitResponse response = createPaymentUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.ok("Payment link created successfully", response));
    }
}
