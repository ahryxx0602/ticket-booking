package com.ticket.payment.infrastructure.persistence.adapter;

import com.ticket.payment.domain.model.Payment;
import com.ticket.payment.domain.model.PaymentMethod;
import com.ticket.payment.domain.model.PaymentRevenueReport;
import com.ticket.payment.domain.model.PaymentStatus;
import com.ticket.payment.domain.repository.PaymentRepository;
import com.ticket.payment.infrastructure.persistence.entity.PaymentJpaEntity;
import com.ticket.payment.infrastructure.persistence.repository.SpringDataPaymentRepository;
import com.ticket.shared.dto.PageResponse;
import com.ticket.shared.exception.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

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
    public Optional<Payment> findByPaymentReferenceForUpdate(String paymentReference) {
        return springDataPaymentRepository.findByPaymentReferenceForUpdate(paymentReference).map(this::toDomain);
    }

    @Override
    public Optional<Payment> findFirstByBookingIdAndStatusOrderByIdDesc(Long bookingId, PaymentStatus status) {
        return springDataPaymentRepository.findFirstByBookingIdAndStatusOrderByIdDesc(bookingId, status).map(this::toDomain);
    }

    @Override
    public Optional<Payment> findFirstByBookingIdAndStatus(Long bookingId, PaymentStatus status) {
        return springDataPaymentRepository.findFirstByBookingIdAndStatus(bookingId, status).map(this::toDomain);
    }

    @Override
    public PaymentRevenueReport getRevenueReport(LocalDateTime from, LocalDateTime to) {
        BigDecimal gross = springDataPaymentRepository.calculateGrossRevenue(from, to);
        BigDecimal refunded = springDataPaymentRepository.calculateTotalRefunded(from, to);
        BigDecimal net = gross.subtract(refunded);

        long total = springDataPaymentRepository.countTotalInPeriod(from, to);
        long successCount = springDataPaymentRepository.countByStatusInPeriod(PaymentStatus.SUCCESS, from, to);
        long failedCount = springDataPaymentRepository.countByStatusInPeriod(PaymentStatus.FAILED, from, to);
        long refundedCount = springDataPaymentRepository.countByStatusInPeriod(PaymentStatus.REFUNDED, from, to);
        long pendingCount = springDataPaymentRepository.countByStatusInPeriod(PaymentStatus.PENDING, from, to);
        long reconciliationCount = springDataPaymentRepository.countReconciliationInPeriod(from, to);

        Map<PaymentMethod, BigDecimal> revenueByMethod = new EnumMap<>(PaymentMethod.class);
        List<Object[]> rawMethods = springDataPaymentRepository.calculateRevenueByMethod(from, to);
        for (Object[] row : rawMethods) {
            PaymentMethod method = (PaymentMethod) row[0];
            BigDecimal rev = (BigDecimal) row[1];
            revenueByMethod.put(method, rev);
        }

        Pageable recPageable = PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<PaymentJpaEntity> recSpec = (root, q, cb) -> cb.and(
                cb.greaterThanOrEqualTo(root.get("createdAt"), from),
                cb.lessThan(root.get("createdAt"), to),
                cb.isTrue(root.get("reconciliationRequired"))
        );
        List<Payment> reconciliationPayments = springDataPaymentRepository.findAll(recSpec, recPageable)
                .getContent().stream().map(this::toDomain).toList();

        return PaymentRevenueReport.builder()
                .grossRevenue(gross)
                .totalRefunded(refunded)
                .netRevenue(net)
                .totalTransactions(total)
                .successCount(successCount)
                .failedCount(failedCount)
                .refundedCount(refundedCount)
                .pendingCount(pendingCount)
                .reconciliationCount(reconciliationCount)
                .revenueByMethod(revenueByMethod)
                .reconciliationPayments(reconciliationPayments)
                .build();
    }

    @Override
    public PageResponse<Payment> findPaymentsWithFilters(PaymentStatus status, PaymentMethod paymentMethod, String paymentReference, Boolean reconciliationRequired, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 100));
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt", "id"));

        Specification<PaymentJpaEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (paymentMethod != null) {
                predicates.add(cb.equal(root.get("paymentMethod"), paymentMethod));
            }
            if (paymentReference != null && !paymentReference.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("paymentReference")), "%" + paymentReference.trim().toLowerCase() + "%"));
            }
            if (reconciliationRequired != null) {
                predicates.add(cb.equal(root.get("reconciliationRequired"), reconciliationRequired));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<PaymentJpaEntity> entityPage = springDataPaymentRepository.findAll(spec, pageable);
        List<Payment> items = entityPage.getContent().stream().map(this::toDomain).toList();

        return PageResponse.<Payment>builder()
                .items(items)
                .page(entityPage.getNumber())
                .size(entityPage.getSize())
                .totalElements(entityPage.getTotalElements())
                .totalPages(entityPage.getTotalPages())
                .hasNext(entityPage.hasNext())
                .hasPrevious(entityPage.hasPrevious())
                .build();
    }

    @Override
    public Payment save(Payment payment) {
        PaymentJpaEntity entity;
        if (payment.getId() != null) {
            entity = springDataPaymentRepository.findById(payment.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Payment", payment.getId()));
            if (payment.getVersion() != null && !payment.getVersion().equals(entity.getVersion())) {
                throw new ObjectOptimisticLockingFailureException(PaymentJpaEntity.class, payment.getId());
            }
        } else {
            entity = new PaymentJpaEntity();
        }

        entity.setPaymentReference(payment.getPaymentReference());
        entity.setBookingId(payment.getBookingId());
        entity.setAmount(payment.getAmount());
        entity.setCurrency(payment.getCurrency() != null ? payment.getCurrency() : "VND");
        entity.setPaymentMethod(payment.getPaymentMethod());
        entity.setTransactionId(payment.getTransactionId());
        entity.setStatus(payment.getStatus());
        entity.setPaymentTime(payment.getPaymentTime());
        entity.setRefundAmount(payment.getRefundAmount());
        entity.setRefundReason(payment.getRefundReason());
        entity.setRefundedAt(payment.getRefundedAt());
        entity.setReconciliationRequired(payment.isReconciliationRequired());

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
                .refundAmount(entity.getRefundAmount())
                .refundReason(entity.getRefundReason())
                .refundedAt(entity.getRefundedAt())
                .reconciliationRequired(entity.isReconciliationRequired())
                .version(entity.getVersion())
                .build();
        p.setCreatedAt(entity.getCreatedAt());
        p.setUpdatedAt(entity.getUpdatedAt());
        return p;
    }
}
