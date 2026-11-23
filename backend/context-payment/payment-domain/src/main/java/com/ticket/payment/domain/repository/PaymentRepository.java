package com.ticket.payment.domain.repository;

import com.ticket.payment.domain.model.Payment;
import com.ticket.payment.domain.model.PaymentMethod;
import com.ticket.payment.domain.model.PaymentRevenueReport;
import com.ticket.payment.domain.model.PaymentStatus;
import com.ticket.shared.dto.PageResponse;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PaymentRepository {
    Optional<Payment> findById(Long id);
    Optional<Payment> findByPaymentReference(String paymentReference);
    Optional<Payment> findByPaymentReferenceForUpdate(String paymentReference);
    Optional<Payment> findFirstByBookingIdAndStatusOrderByIdDesc(Long bookingId, PaymentStatus status);
    Optional<Payment> findFirstByBookingIdAndStatus(Long bookingId, PaymentStatus status);
    PaymentRevenueReport getRevenueReport(LocalDateTime from, LocalDateTime to);
    PageResponse<Payment> findPaymentsWithFilters(PaymentStatus status, PaymentMethod paymentMethod, String paymentReference, Boolean reconciliationRequired, int page, int size);

    default PageResponse<Payment> findPaymentsWithFilters(PaymentStatus status, PaymentMethod paymentMethod, String paymentReference, int page, int size) {
        return findPaymentsWithFilters(status, paymentMethod, paymentReference, null, page, size);
    }
    Payment save(Payment payment);
}
