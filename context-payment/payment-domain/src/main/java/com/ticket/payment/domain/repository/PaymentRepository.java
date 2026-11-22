package com.ticket.payment.domain.repository;

import com.ticket.payment.domain.model.Payment;
import com.ticket.payment.domain.model.PaymentStatus;

import java.util.Optional;

public interface PaymentRepository {
    Optional<Payment> findById(Long id);
    Optional<Payment> findByPaymentReference(String paymentReference);
    Optional<Payment> findFirstByBookingIdAndStatusOrderByIdDesc(Long bookingId, PaymentStatus status);
    Payment save(Payment payment);
}
