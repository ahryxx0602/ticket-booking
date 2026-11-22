package com.ticket.payment.infrastructure.persistence.repository;

import com.ticket.payment.domain.model.PaymentStatus;
import com.ticket.payment.infrastructure.persistence.entity.PaymentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpringDataPaymentRepository extends JpaRepository<PaymentJpaEntity, Long> {
    Optional<PaymentJpaEntity> findByPaymentReference(String paymentReference);
    Optional<PaymentJpaEntity> findFirstByBookingIdAndStatusOrderByIdDesc(Long bookingId, PaymentStatus status);
}
