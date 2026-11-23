package com.ticket.payment.presentation.controller;

import com.ticket.payment.application.dto.VnPayCallbackDto;
import com.ticket.payment.application.dto.VnPayIpnResponse;
import com.ticket.payment.application.usecase.HandlePaymentCallbackUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments/webhook")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final HandlePaymentCallbackUseCase handlePaymentCallbackUseCase;

    @GetMapping(value = "/vnpay", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VnPayIpnResponse> vnpayCallback(@RequestParam Map<String, String> allParams) {
        VnPayCallbackDto dto = new VnPayCallbackDto();
        dto.setVnp_TxnRef(allParams.get("vnp_TxnRef"));
        dto.setVnp_Amount(allParams.get("vnp_Amount"));
        dto.setVnp_TransactionNo(allParams.get("vnp_TransactionNo"));
        dto.setVnp_ResponseCode(allParams.get("vnp_ResponseCode"));
        dto.setVnp_TransactionStatus(allParams.get("vnp_TransactionStatus"));
        dto.setVnp_SecureHash(allParams.get("vnp_SecureHash"));
        dto.setAllParams(allParams);

        VnPayIpnResponse response = handlePaymentCallbackUseCase.execute(dto);
        return ResponseEntity.ok(response);
    }
}
