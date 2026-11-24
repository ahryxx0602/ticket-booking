package com.ticket.payment.presentation.controller;

import com.ticket.payment.application.dto.PaymentDetailResponse;
import com.ticket.payment.application.dto.PaymentReportResponse;
import com.ticket.payment.application.usecase.AdminGetPaymentsUseCase;
import com.ticket.payment.application.usecase.GetPaymentReportUseCase;
import com.ticket.payment.domain.model.PaymentMethod;
import com.ticket.payment.domain.model.PaymentStatus;
import com.ticket.shared.dto.ApiResponse;
import com.ticket.shared.dto.PageResponse;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminPaymentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GetPaymentReportUseCase getPaymentReportUseCase;
    @Mock
    private AdminGetPaymentsUseCase adminGetPaymentsUseCase;
    @Mock
    private com.ticket.payment.application.usecase.AdminReconcilePaymentUseCase adminReconcilePaymentUseCase;

    @InjectMocks
    private AdminPaymentController adminPaymentController;

    @BeforeEach
    void setUp() {
        adminPaymentController.setAdminReconcilePaymentUseCase(adminReconcilePaymentUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(adminPaymentController)
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
    void getPaymentReport_shouldReturnOkWithReportData() throws Exception {
        PaymentReportResponse report = PaymentReportResponse.builder()
                .grossRevenue(BigDecimal.valueOf(5000000))
                .totalRefunded(BigDecimal.valueOf(1000000))
                .netRevenue(BigDecimal.valueOf(4000000))
                .totalTransactions(5)
                .successCount(4)
                .refundedCount(1)
                .revenueByMethod(Map.of("VNPAY", BigDecimal.valueOf(5000000)))
                .build();

        when(getPaymentReportUseCase.execute(any(), any())).thenReturn(report);

        mockMvc.perform(get("/api/v1/admin/payments/reports")
                        .param("startDate", "2026-11-01")
                        .param("endDate", "2026-11-30")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.netRevenue").value(4000000))
                .andExpect(jsonPath("$.data.totalTransactions").value(5));
    }

    @Test
    void getPayments_shouldReturnPagedPayments() throws Exception {
        PaymentDetailResponse detail = PaymentDetailResponse.builder()
                .id(1L)
                .paymentReference("PAY_ADMIN_01")
                .amount(BigDecimal.valueOf(500000))
                .status(PaymentStatus.SUCCESS)
                .paymentMethod(PaymentMethod.VNPAY)
                .build();

        PageResponse<PaymentDetailResponse> page = PageResponse.<PaymentDetailResponse>builder()
                .items(List.of(detail))
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .build();

        when(adminGetPaymentsUseCase.execute(any(), any(), any(), any(), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/payments")
                        .param("reconciliationRequired", "true")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].paymentReference").value("PAY_ADMIN_01"));
    }

    @Test
    void getPaymentReport_whenForbidden_shouldReturn403AccessDenied() throws Exception {
        when(getPaymentReportUseCase.execute(any(), any()))
                .thenThrow(new ForbiddenException("ACCESS_DENIED", "Admin privileges required"));

        mockMvc.perform(get("/api/v1/admin/payments/reports")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void resolveReconciliation_shouldReturnOk() throws Exception {
        PaymentDetailResponse resolved = PaymentDetailResponse.builder()
                .id(1L)
                .paymentReference("PAY_REC_01")
                .reconciliationRequired(false)
                .build();
        when(adminReconcilePaymentUseCase.execute(org.mockito.ArgumentMatchers.eq(1L), any())).thenReturn(resolved);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/admin/payments/1/reconcile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"Verified with VNPay\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reconciliationRequired").value(false));
    }
}
