package com.ticket.payment.domain.repository;

import com.ticket.payment.domain.model.Payment;
import java.util.Optional;

public interface PaymentRepository {
    Optional<Payment> findById(Long id);
    Optional<Payment> findByPaymentReference(String paymentReference);
    Payment save(Payment payment);
}
