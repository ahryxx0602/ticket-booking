package com.ticket.payment.infrastructure.persistence.adapter;

import com.ticket.payment.domain.model.Payment;
import com.ticket.payment.domain.repository.PaymentRepository;
import com.ticket.payment.infrastructure.persistence.entity.PaymentJpaEntity;
import com.ticket.payment.infrastructure.persistence.repository.SpringDataPaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepository {

    private final SpringDataPaymentRepository springDataPaymentRepository;

    @Override
    public Optional<Payment> findById(Long id) {
        return springDataPaymentRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Payment> findByPaymentReference(String paymentReference) {
        return springDataPaymentRepository.findByPaymentReference(paymentReference).map(this::toDomain);
    }

    @Override
    public Payment save(Payment payment) {
        PaymentJpaEntity entity = toEntity(payment);
        PaymentJpaEntity saved = springDataPaymentRepository.save(entity);
        return toDomain(saved);
    }

    private Payment toDomain(PaymentJpaEntity entity) {
        Payment p = Payment.builder()
                .id(entity.getId())
                .paymentReference(entity.getPaymentReference())
                .bookingId(entity.getBookingId())
                .amount(entity.getAmount())
                .currency(entity.getCurrency())
                .paymentMethod(entity.getPaymentMethod())
                .transactionId(entity.getTransactionId())
                .status(entity.getStatus())
                .paymentTime(entity.getPaymentTime())
                .build();
        p.setCreatedAt(entity.getCreatedAt());
        p.setUpdatedAt(entity.getUpdatedAt());
        return p;
    }

    private PaymentJpaEntity toEntity(Payment payment) {
        PaymentJpaEntity entity = new PaymentJpaEntity();
        entity.setId(payment.getId());
        entity.setPaymentReference(payment.getPaymentReference());
        entity.setBookingId(payment.getBookingId());
        entity.setAmount(payment.getAmount());
        entity.setCurrency(payment.getCurrency() != null ? payment.getCurrency() : "VND");
        entity.setPaymentMethod(payment.getPaymentMethod());
        entity.setTransactionId(payment.getTransactionId());
        entity.setStatus(payment.getStatus());
        entity.setPaymentTime(payment.getPaymentTime());
        return entity;
    }
}
