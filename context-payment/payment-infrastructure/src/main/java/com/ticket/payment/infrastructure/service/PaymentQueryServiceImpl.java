package com.ticket.payment.infrastructure.service;

import com.ticket.payment.application.dto.PaymentDetailResponse;
import com.ticket.payment.application.dto.PaymentReportResponse;
import com.ticket.payment.application.usecase.AdminGetPaymentsUseCase;
import com.ticket.payment.application.usecase.GetPaymentReportUseCase;
import com.ticket.payment.domain.model.Payment;
import com.ticket.payment.domain.model.PaymentMethod;
import com.ticket.payment.domain.model.PaymentRevenueReport;
import com.ticket.payment.domain.model.PaymentStatus;
import com.ticket.payment.domain.repository.PaymentRepository;
import com.ticket.shared.dto.PageResponse;
import com.ticket.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentQueryServiceImpl implements GetPaymentReportUseCase, AdminGetPaymentsUseCase {

    private final PaymentRepository paymentRepository;

    @Override
    @Transactional(readOnly = true)
    public PaymentReportResponse execute(LocalDate startDate, LocalDate endDate) {
        LocalDate start = (startDate != null) ? startDate : LocalDate.now().minusDays(30);
        LocalDate end = (endDate != null) ? endDate : LocalDate.now();

        if (start.isAfter(end)) {
            throw new BusinessException("INVALID_DATE_RANGE", "Start date must be before or equal to end date");
        }

        LocalDateTime from = start.atStartOfDay();
        LocalDateTime to = end.plusDays(1).atStartOfDay();

        PaymentRevenueReport report = paymentRepository.getRevenueReport(from, to);

        Map<String, BigDecimal> methodMap = new HashMap<>();
        if (report.getRevenueByMethod() != null) {
            report.getRevenueByMethod().forEach((k, v) -> methodMap.put(k.name(), v));
        }

        List<PaymentDetailResponse> recTransactions = report.getReconciliationPayments() != null
                ? report.getReconciliationPayments().stream().map(this::toResponse).toList()
                : Collections.emptyList();

        return PaymentReportResponse.builder()
                .grossRevenue(report.getGrossRevenue())
                .totalRefunded(report.getTotalRefunded())
                .netRevenue(report.getNetRevenue())
                .totalTransactions(report.getTotalTransactions())
                .successCount(report.getSuccessCount())
                .failedCount(report.getFailedCount())
                .refundedCount(report.getRefundedCount())
                .pendingCount(report.getPendingCount())
                .reconciliationCount(report.getReconciliationCount())
                .revenueByMethod(methodMap)
                .reconciliationTransactions(recTransactions)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PaymentDetailResponse> execute(PaymentStatus status, PaymentMethod paymentMethod, String paymentReference, Boolean reconciliationRequired, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 100));

        PageResponse<Payment> paymentPage = paymentRepository.findPaymentsWithFilters(status, paymentMethod, paymentReference, reconciliationRequired, safePage, safeSize);
        List<PaymentDetailResponse> items = paymentPage.getItems().stream().map(this::toResponse).toList();

        return PageResponse.<PaymentDetailResponse>builder()
                .items(items)
                .page(paymentPage.getPage())
                .size(paymentPage.getSize())
                .totalElements(paymentPage.getTotalElements())
                .totalPages(paymentPage.getTotalPages())
                .hasNext(paymentPage.isHasNext())
                .hasPrevious(paymentPage.isHasPrevious())
                .build();
    }

    private PaymentDetailResponse toResponse(Payment p) {
        return PaymentDetailResponse.builder()
                .id(p.getId())
                .paymentReference(p.getPaymentReference())
                .bookingId(p.getBookingId())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .paymentMethod(p.getPaymentMethod())
                .transactionId(p.getTransactionId())
                .status(p.getStatus())
                .paymentTime(p.getPaymentTime())
                .refundAmount(p.getRefundAmount())
                .refundReason(p.getRefundReason())
                .refundedAt(p.getRefundedAt())
                .reconciliationRequired(p.isReconciliationRequired())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
