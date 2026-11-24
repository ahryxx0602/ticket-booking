package com.ticket.payment.presentation.controller;

import com.ticket.payment.application.dto.PaymentDetailResponse;
import com.ticket.payment.application.dto.PaymentReportResponse;
import com.ticket.payment.application.usecase.AdminGetPaymentsUseCase;
import com.ticket.payment.application.usecase.AdminReconcilePaymentUseCase;
import com.ticket.payment.application.usecase.GetPaymentReportUseCase;
import com.ticket.payment.domain.model.PaymentMethod;
import com.ticket.payment.domain.model.PaymentStatus;
import com.ticket.shared.dto.ApiResponse;
import com.ticket.shared.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/payments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPaymentController {

    private final GetPaymentReportUseCase getPaymentReportUseCase;
    private final AdminGetPaymentsUseCase adminGetPaymentsUseCase;

    private AdminReconcilePaymentUseCase adminReconcilePaymentUseCase;

    @Autowired(required = false)
    public void setAdminReconcilePaymentUseCase(AdminReconcilePaymentUseCase adminReconcilePaymentUseCase) {
        this.adminReconcilePaymentUseCase = adminReconcilePaymentUseCase;
    }

    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<PaymentReportResponse>> getPaymentReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        PaymentReportResponse response = getPaymentReportUseCase.execute(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.ok("Payment report retrieved successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PaymentDetailResponse>>> getPayments(
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(required = false) String paymentReference,
            @RequestParam(required = false) Boolean reconciliationRequired,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<PaymentDetailResponse> response = adminGetPaymentsUseCase.execute(status, paymentMethod, paymentReference, reconciliationRequired, page, size);
        return ResponseEntity.ok(ApiResponse.ok("Payments retrieved successfully", response));
    }

    @PostMapping("/{id}/reconcile")
    public ResponseEntity<ApiResponse<PaymentDetailResponse>> resolveReconciliation(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String note = body != null ? body.get("note") : null;
        PaymentDetailResponse response = adminReconcilePaymentUseCase.execute(id, note);
        return ResponseEntity.ok(ApiResponse.ok("Reconciliation alert resolved successfully", response));
    }
}
