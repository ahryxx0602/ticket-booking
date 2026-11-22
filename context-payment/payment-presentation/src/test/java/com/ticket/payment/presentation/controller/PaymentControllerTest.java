package com.ticket.payment.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.payment.application.dto.CreatePaymentCommand;
import com.ticket.payment.application.dto.PaymentInitResponse;
import com.ticket.payment.application.usecase.CreatePaymentUseCase;
import com.ticket.payment.domain.model.PaymentMethod;
import com.ticket.shared.dto.ApiResponse;
import com.ticket.shared.exception.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.security.Principal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private CreatePaymentUseCase createPaymentUseCase;

    @InjectMocks
    private PaymentController paymentController;

    private final Principal customerPrincipal = () -> "customer1";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(paymentController)
                .setControllerAdvice(new TestExceptionHandler())
                .build();
    }

    @ControllerAdvice
    static class TestExceptionHandler {
        @ExceptionHandler(ForbiddenException.class)
        public ResponseEntity<ApiResponse<Void>> handleForbidden(ForbiddenException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error(ex.getCode(), ex.getMessage()));
        }
    }

    @Test
    void createPayment_whenValid_shouldReturnOkWithRedirectUrl() throws Exception {
        CreatePaymentCommand command = CreatePaymentCommand.builder()
                .bookingReference("BKG_123456")
                .paymentMethod(PaymentMethod.VNPAY)
                .build();

        PaymentInitResponse response = PaymentInitResponse.builder()
                .paymentReference("PAY_123456789")
                .redirectPaymentUrl("https://sandbox.vnpayment.vn/pay?param=1")
                .build();

        when(createPaymentUseCase.execute(any(CreatePaymentCommand.class), eq("customer1"), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/payments/checkout")
                        .principal(customerPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentReference").value("PAY_123456789"))
                .andExpect(jsonPath("$.data.redirectPaymentUrl").value("https://sandbox.vnpayment.vn/pay?param=1"));
    }

    @Test
    void createPayment_whenValidWithStandardUuid_shouldReturnOk() throws Exception {
        CreatePaymentCommand command = CreatePaymentCommand.builder()
                .bookingReference("550e8400-e29b-41d4-a716-446655440000")
                .paymentMethod(PaymentMethod.VNPAY)
                .build();

        PaymentInitResponse response = PaymentInitResponse.builder()
                .paymentReference("PAY_UUID_TEST")
                .redirectPaymentUrl("https://sandbox.vnpayment.vn/pay?param=uuid")
                .build();

        when(createPaymentUseCase.execute(any(CreatePaymentCommand.class), eq("customer1"), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/payments/checkout")
                        .principal(customerPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentReference").value("PAY_UUID_TEST"));
    }

    @Test
    void createPayment_whenForbiddenException_shouldReturn403BookingAccessDenied() throws Exception {
        CreatePaymentCommand command = CreatePaymentCommand.builder()
                .bookingReference("BKG_FORBIDDEN_01")
                .paymentMethod(PaymentMethod.VNPAY)
                .build();

        when(createPaymentUseCase.execute(any(CreatePaymentCommand.class), eq("customer1"), any()))
                .thenThrow(new ForbiddenException("BOOKING_ACCESS_DENIED", "Access denied"));

        mockMvc.perform(post("/api/v1/payments/checkout")
                        .principal(customerPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("BOOKING_ACCESS_DENIED"));
    }

    @Test
    void createPayment_whenMissingBookingReference_shouldReturnBadRequest() throws Exception {
        CreatePaymentCommand command = CreatePaymentCommand.builder()
                .paymentMethod(PaymentMethod.VNPAY)
                .build();

        mockMvc.perform(post("/api/v1/payments/checkout")
                        .principal(customerPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isBadRequest());
    }
}
