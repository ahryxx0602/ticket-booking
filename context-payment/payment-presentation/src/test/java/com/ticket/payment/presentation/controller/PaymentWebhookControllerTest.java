package com.ticket.payment.presentation.controller;

import com.ticket.payment.application.dto.VnPayCallbackDto;
import com.ticket.payment.application.dto.VnPayIpnResponse;
import com.ticket.payment.application.usecase.HandlePaymentCallbackUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PaymentWebhookControllerTest {

    private MockMvc mockMvc;

    @Mock
    private HandlePaymentCallbackUseCase handlePaymentCallbackUseCase;

    @InjectMocks
    private PaymentWebhookController paymentWebhookController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(paymentWebhookController).build();
    }

    @Test
    void vnpayCallback_whenSuccess_shouldReturnRspCode00() throws Exception {
        when(handlePaymentCallbackUseCase.execute(any(VnPayCallbackDto.class)))
                .thenReturn(VnPayIpnResponse.success());

        mockMvc.perform(get("/api/v1/payments/webhook/vnpay")
                        .param("vnp_TxnRef", "PAY_001")
                        .param("vnp_Amount", "150000000")
                        .param("vnp_ResponseCode", "00")
                        .param("vnp_TransactionNo", "14567890")
                        .param("vnp_SecureHash", "mock_hash")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.RspCode").value("00"))
                .andExpect(jsonPath("$.Message").value("Confirm Success"));
    }

    @Test
    void vnpayCallback_whenInvalidChecksum_shouldReturnRspCode97() throws Exception {
        when(handlePaymentCallbackUseCase.execute(any(VnPayCallbackDto.class)))
                .thenReturn(VnPayIpnResponse.invalidChecksum());

        mockMvc.perform(get("/api/v1/payments/webhook/vnpay")
                        .param("vnp_TxnRef", "PAY_002")
                        .param("vnp_SecureHash", "bad_hash")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("97"))
                .andExpect(jsonPath("$.Message").value("Invalid Checksum"));
    }
}
