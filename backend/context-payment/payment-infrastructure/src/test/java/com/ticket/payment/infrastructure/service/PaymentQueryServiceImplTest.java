package com.ticket.payment.infrastructure.service;

import com.ticket.payment.application.dto.PaymentDetailResponse;
import com.ticket.payment.application.dto.PaymentReportResponse;
import com.ticket.payment.domain.model.Payment;
import com.ticket.payment.domain.model.PaymentMethod;
import com.ticket.payment.domain.model.PaymentRevenueReport;
import com.ticket.payment.domain.model.PaymentStatus;
import com.ticket.payment.domain.repository.PaymentRepository;
import com.ticket.shared.dto.PageResponse;
import com.ticket.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentQueryServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private com.ticket.payment.domain.port.PaymentBookingPort paymentBookingPort;
    @Mock
    private com.ticket.payment.domain.port.PaymentIdentityPort paymentIdentityPort;
    @Mock
    private com.ticket.payment.infrastructure.config.VnPayProperties vnPayProperties;

    private PaymentQueryServiceImpl paymentQueryService;

    @BeforeEach
    void setUp() {
        paymentQueryService = new PaymentQueryServiceImpl(paymentRepository, paymentBookingPort, paymentIdentityPort, vnPayProperties);
    }

    @Test
    void getRevenueReport_whenValidDateRange_shouldReturnCorrectReport() {
        LocalDate start = LocalDate.of(2026, 11, 1);
        LocalDate end = LocalDate.of(2026, 11, 30);

        PaymentRevenueReport domainReport = PaymentRevenueReport.builder()
                .grossRevenue(BigDecimal.valueOf(10000000))
                .totalRefunded(BigDecimal.valueOf(2000000))
                .netRevenue(BigDecimal.valueOf(8000000))
                .totalTransactions(10)
                .successCount(7)
                .failedCount(1)
                .refundedCount(2)
                .pendingCount(0)
                .reconciliationCount(1)
                .revenueByMethod(Map.of(PaymentMethod.VNPAY, BigDecimal.valueOf(10000000)))
                .build();

        when(paymentRepository.getRevenueReport(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(domainReport);

        PaymentReportResponse response = paymentQueryService.execute(start, end);

        assertThat(response).isNotNull();
        assertThat(response.getGrossRevenue()).isEqualByComparingTo(BigDecimal.valueOf(10000000));
        assertThat(response.getTotalRefunded()).isEqualByComparingTo(BigDecimal.valueOf(2000000));
        assertThat(response.getNetRevenue()).isEqualByComparingTo(BigDecimal.valueOf(8000000));
        assertThat(response.getRevenueByMethod()).containsEntry("VNPAY", BigDecimal.valueOf(10000000));
    }

    @Test
    void getRevenueReport_whenStartDateAfterEndDate_shouldThrowInvalidDateRange() {
        LocalDate start = LocalDate.of(2026, 12, 1);
        LocalDate end = LocalDate.of(2026, 11, 1);

        assertThatThrownBy(() -> paymentQueryService.execute(start, end))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("INVALID_DATE_RANGE");
    }

    @Test
    void getPaymentsWithFilters_shouldClampMaxPageSize() {
        Payment p = Payment.builder()
                .id(1L)
                .paymentReference("PAY_001")
                .bookingId(10L)
                .amount(BigDecimal.valueOf(500000))
                .status(PaymentStatus.SUCCESS)
                .build();

        PageResponse<Payment> domainPage = PageResponse.<Payment>builder()
                .items(List.of(p))
                .page(0)
                .size(100)
                .totalElements(1)
                .totalPages(1)
                .build();

        when(paymentRepository.findPaymentsWithFilters(PaymentStatus.SUCCESS, PaymentMethod.VNPAY, null, null, 0, 100))
                .thenReturn(domainPage);

        PageResponse<PaymentDetailResponse> response = paymentQueryService.execute(
                PaymentStatus.SUCCESS,
                PaymentMethod.VNPAY,
                null,
                0,
                500 // Requesting 500 should be clamped to 100
        );

        assertThat(response).isNotNull();
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().getFirst().getPaymentReference()).isEqualTo("PAY_001");
    }
}
