package com.ticket.payment.infrastructure.service;

import com.ticket.payment.application.dto.CustomerPaymentStatusResponse;
import com.ticket.payment.application.dto.PaymentDetailResponse;
import com.ticket.payment.application.dto.PaymentReportResponse;
import com.ticket.payment.application.usecase.AdminGetPaymentsUseCase;
import com.ticket.payment.application.usecase.AdminReconcilePaymentUseCase;
import com.ticket.payment.application.usecase.GetCustomerPaymentStatusUseCase;
import com.ticket.payment.application.usecase.GetPaymentReportUseCase;
import com.ticket.payment.application.usecase.HandleVnPayBrowserReturnUseCase;
import com.ticket.payment.domain.model.Payment;
import com.ticket.payment.domain.model.PaymentMethod;
import com.ticket.payment.domain.model.PaymentRevenueReport;
import com.ticket.payment.domain.model.PaymentStatus;
import com.ticket.payment.domain.port.PaymentBookingPort;
import com.ticket.payment.domain.port.PaymentIdentityPort;
import com.ticket.payment.domain.repository.PaymentRepository;
import com.ticket.payment.infrastructure.config.VnPayProperties;
import com.ticket.payment.infrastructure.crypto.VnPaySignUtil;
import com.ticket.shared.dto.PageResponse;
import com.ticket.shared.exception.BusinessException;
import com.ticket.shared.exception.EntityNotFoundException;
import com.ticket.shared.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentQueryServiceImpl implements GetPaymentReportUseCase, AdminGetPaymentsUseCase, AdminReconcilePaymentUseCase, GetCustomerPaymentStatusUseCase, HandleVnPayBrowserReturnUseCase {

    private final PaymentRepository paymentRepository;
    private final PaymentBookingPort paymentBookingPort;
    private final PaymentIdentityPort paymentIdentityPort;
    private final VnPayProperties vnPayProperties;

    @Override
    @Transactional(readOnly = true)
    public CustomerPaymentStatusResponse execute(String bookingReference, String authenticatedUsername) {
        if (bookingReference == null || bookingReference.isBlank()) {
            throw new BusinessException("INVALID_BOOKING_REFERENCE", "Mã tham chiếu đơn đặt chỗ không hợp lệ");
        }
        Long userId = paymentIdentityPort.findActiveUserIdByUsername(authenticatedUsername)
                .orElseThrow(() -> new ForbiddenException("User not found or inactive"));

        PaymentBookingPort.BookingSnapshot booking = paymentBookingPort.findBookingByReference(bookingReference.trim())
                .orElseThrow(() -> new EntityNotFoundException("Booking", bookingReference));

        if (!userId.equals(booking.userId())) {
            throw new ForbiddenException("Access denied: You do not own this booking");
        }

        List<Payment> payments = paymentRepository.findAllByBookingIdOrderByIdDesc(booking.id());

        if (payments.isEmpty()) {
            return CustomerPaymentStatusResponse.builder()
                    .bookingReference(booking.bookingReference())
                    .bookingStatus(booking.status())
                    .hasPaymentAttempt(false)
                    .hasSuccessfulPayment(false)
                    .totalAttempts(0)
                    .canAttemptPayment("HELD".equalsIgnoreCase(booking.status()))
                    .amount(booking.totalAmount())
                    .currency("VND")
                    .reconciliationRequired(false)
                    .build();
        }

        // BLOCKER-02: Deterministic multiple attempts aggregation
        boolean anyReconciliationRequired = payments.stream().anyMatch(Payment::isReconciliationRequired);
        Optional<Payment> successfulPayment = payments.stream().filter(p -> p.getStatus() == PaymentStatus.SUCCESS).findFirst();
        Optional<Payment> pendingPayment = payments.stream().filter(p -> p.getStatus() == PaymentStatus.PENDING).findFirst();

        // Priority: SUCCESS > PENDING > Most Recent
        Payment selectedPayment = successfulPayment.orElseGet(() -> pendingPayment.orElseGet(() -> payments.get(0)));
        boolean hasSuccessful = successfulPayment.isPresent();
        boolean canAttempt = !hasSuccessful && "HELD".equalsIgnoreCase(booking.status());

        return CustomerPaymentStatusResponse.builder()
                .bookingReference(booking.bookingReference())
                .bookingStatus(booking.status())
                .hasPaymentAttempt(true)
                .hasSuccessfulPayment(hasSuccessful)
                .totalAttempts(payments.size())
                .canAttemptPayment(canAttempt)
                .activePaymentReference(pendingPayment.map(Payment::getPaymentReference).orElse(null))
                .paymentReference(selectedPayment.getPaymentReference())
                .paymentStatus(selectedPayment.getStatus())
                .paymentMethod(selectedPayment.getPaymentMethod())
                .amount(selectedPayment.getAmount())
                .currency(selectedPayment.getCurrency() != null ? selectedPayment.getCurrency() : "VND")
                .reconciliationRequired(anyReconciliationRequired)
                .createdAt(selectedPayment.getCreatedAt())
                .updatedAt(selectedPayment.getPaymentTime() != null ? selectedPayment.getPaymentTime() : selectedPayment.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public String execute(Map<String, String> allParams) {
        String baseReturnUrl = vnPayProperties.getFrontendReturnUrl();
        if (baseReturnUrl == null || baseReturnUrl.isBlank() || (!baseReturnUrl.startsWith("http://") && !baseReturnUrl.startsWith("https://"))) {
            baseReturnUrl = "http://localhost:5173/checkout";
        }
        String cleanBase = baseReturnUrl.endsWith("/") ? baseReturnUrl.substring(0, baseReturnUrl.length() - 1) : baseReturnUrl;

        if (allParams == null || allParams.isEmpty()) {
            return cleanBase + "?vnp_Error=EMPTY_CALLBACK";
        }

        // BLOCKER-06: Validate signature BEFORE querying or revealing bookingReference
        boolean validSignature = VnPaySignUtil.verifySignature(allParams, vnPayProperties.getHashSecret());
        String txnRef = allParams.get("vnp_TxnRef");
        String responseCode = allParams.get("vnp_ResponseCode");

        if (!validSignature) {
            log.warn("[VNPAY BROWSER RETURN] Invalid or unverified signature. Rejecting without correlating payment/booking.");
            return cleanBase + "?vnp_Error=INVALID_SIGNATURE";
        }

        // Only with valid signature: correlate to booking reference
        String targetBookingReference = "";
        if (txnRef != null && !txnRef.isBlank()) {
            Optional<Payment> paymentOpt = paymentRepository.findByPaymentReference(txnRef.trim());
            if (paymentOpt.isPresent()) {
                Optional<PaymentBookingPort.BookingSnapshot> bookingOpt = paymentBookingPort.findBookingById(paymentOpt.get().getBookingId());
                if (bookingOpt.isPresent()) {
                    targetBookingReference = bookingOpt.get().bookingReference();
                }
            }
        }

        StringBuilder redirectUrl = new StringBuilder(cleanBase);
        if (!targetBookingReference.isBlank()) {
            redirectUrl.append("/").append(URLEncoder.encode(targetBookingReference, StandardCharsets.UTF_8));
        }

        redirectUrl.append("?vnp_ResponseCode=").append(responseCode != null ? URLEncoder.encode(responseCode, StandardCharsets.UTF_8) : "")
                .append("&vnp_TxnRef=").append(txnRef != null ? URLEncoder.encode(txnRef, StandardCharsets.UTF_8) : "");

        if (targetBookingReference.isBlank() && txnRef != null && !txnRef.isBlank()) {
            redirectUrl.append("&vnp_Error=BOOKING_NOT_FOUND");
        }

        return redirectUrl.toString();
    }

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

    @Override
    @Transactional
    public PaymentDetailResponse execute(Long paymentId, String resolutionNote) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new EntityNotFoundException("Payment", paymentId));
        payment.resolveReconciliation(resolutionNote);
        Payment saved = paymentRepository.save(payment);
        return toResponse(saved);
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
